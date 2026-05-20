package com.example.mdreader.controller;

import com.example.mdreader.model.DocumentSource;
import com.example.mdreader.model.ReaderPreferences;
import com.example.mdreader.model.ReaderState;
import com.example.mdreader.model.RenderedDocument;
import com.example.mdreader.model.Theme;
import com.example.mdreader.model.TocItem;
import com.example.mdreader.service.FileManager;
import com.example.mdreader.service.HtmlTemplateService;
import com.example.mdreader.service.MarkdownService;
import com.example.mdreader.service.PreferenceService;
import com.example.mdreader.service.SearchService;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ReaderController {
    private final Stage stage;
    private final FileManager fileManager;
    private final MarkdownService markdownService;
    private final HtmlTemplateService htmlTemplateService;
    private final PreferenceService preferenceService;
    private final SearchService searchService;

    private final ReaderState readerState = new ReaderState();
    private ReaderPreferences preferences;

    private TreeView<TocItem> tocTree;
    private WebView webView;
    private Label statusLabel;
    private TextField searchField;
    private boolean suppressTocNavigation;

    public ReaderController(Stage stage) {
        this.stage = stage;
        this.fileManager = new FileManager();
        this.markdownService = new MarkdownService();
        this.htmlTemplateService = new HtmlTemplateService();
        this.preferenceService = new PreferenceService();
        this.searchService = new SearchService();
        this.preferences = preferenceService.load();
    }

    public Parent createContent() {
        BorderPane root = new BorderPane();
        root.setTop(buildToolBar());
        root.setCenter(buildCenter());
        root.setBottom(buildStatusBar());
        installDragAndDrop(root);

        loadWelcomePage();
        return root;
    }

    public ReaderPreferences preferences() {
        return preferences;
    }

    private ToolBar buildToolBar() {
        Button openButton = new Button("打开");
        openButton.setOnAction(event -> openFileChooser());

        Button reloadButton = new Button("刷新");
        reloadButton.setOnAction(event -> reloadCurrentFile());

        Button themeButton = new Button("主题");
        themeButton.setOnAction(event -> toggleTheme());

        Button smallerFontButton = new Button("A-");
        smallerFontButton.setOnAction(event -> adjustFontSize(-1));

        Button largerFontButton = new Button("A+");
        largerFontButton.setOnAction(event -> adjustFontSize(1));

        searchField = new TextField();
        searchField.setPromptText("搜索当前文档");
        searchField.setOnAction(event -> search(searchField.getText()));

        Button prevMatchButton = new Button("上一个");
        prevMatchButton.setOnAction(event -> searchService.findPrevious(webEngine()));

        Button nextMatchButton = new Button("下一个");
        nextMatchButton.setOnAction(event -> searchService.findNext(webEngine()));

        HBox.setHgrow(searchField, Priority.ALWAYS);
        HBox searchBox = new HBox(8, searchField, prevMatchButton, nextMatchButton);
        searchBox.setPadding(Insets.EMPTY);

        return new ToolBar(
                openButton,
                reloadButton,
                themeButton,
                smallerFontButton,
                largerFontButton,
                new Label("搜索"),
                searchBox
        );
    }

    private SplitPane buildCenter() {
        tocTree = new TreeView<>();
        tocTree.setMinWidth(220);
        tocTree.setPrefWidth(280);
        tocTree.setShowRoot(false);
        tocTree.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (suppressTocNavigation) {
                return;
            }
            if (newItem != null && newItem.getValue() != null) {
                jumpToAnchor(newItem.getValue().anchorId());
            }
        });

        webView = new WebView();
        webView.setContextMenuEnabled(false);
        webView.getEngine().locationProperty().addListener((obs, oldLocation, newLocation) -> {
            if (newLocation == null || newLocation.isBlank()) {
                return;
            }
            if (newLocation.endsWith(".md") || newLocation.endsWith(".markdown") || newLocation.endsWith(".mdown")) {
                openFile(Path.of(java.net.URI.create(newLocation)));
            }
        });

        SplitPane splitPane = new SplitPane(tocTree, webView);
        splitPane.setDividerPositions(0.24);
        return splitPane;
    }

    private void installDragAndDrop(BorderPane root) {
        root.setOnDragOver(event -> {
            Dragboard dragboard = event.getDragboard();
            if (hasMarkdownFile(dragboard)) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        root.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (hasMarkdownFile(dragboard)) {
                Path markdownFile = dragboard.getFiles().stream()
                        .map(File::toPath)
                        .filter(fileManager::isMarkdownFile)
                        .findFirst()
                        .orElse(null);
                if (markdownFile != null) {
                    openFile(markdownFile);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private HBox buildStatusBar() {
        statusLabel = new Label("准备就绪");
        HBox box = new HBox(statusLabel);
        box.setPadding(new Insets(6, 10, 6, 10));
        return box;
    }

    private void openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("打开 Markdown 文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Markdown", "*.md", "*.markdown", "*.mdown")
        );

        java.io.File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            openFile(file.toPath());
        }
    }

    private boolean hasMarkdownFile(Dragboard dragboard) {
        return dragboard.hasFiles() && dragboard.getFiles().stream()
                .map(File::toPath)
                .anyMatch(fileManager::isMarkdownFile);
    }

    public void openFile(Path path) {
        try {
            DocumentSource source = fileManager.readMarkdown(path);
            RenderedDocument rendered = markdownService.render(source);
            String html = htmlTemplateService.buildHtml(rendered, preferences);

            readerState.setCurrentFile(path);
            readerState.setCurrentDocument(rendered);
            webEngine().loadContent(html);
            updateToc(rendered.tocItems());
            selectAnchor(rendered.tocItems().isEmpty() ? null : rendered.tocItems().getFirst().anchorId());
            preferenceService.rememberFile(preferences, path);
            preferenceService.save(preferences);
            statusLabel.setText("已打开: " + path.getFileName());
            stage.setTitle("md-reader - " + path.getFileName());
        } catch (Exception ex) {
            statusLabel.setText("打开失败: " + ex.getMessage());
            loadErrorPage(ex.getMessage());
            showError(ex.getMessage());
        }
    }

    public void reloadCurrentFile() {
        Path currentFile = readerState.getCurrentFile();
        if (currentFile != null) {
            openFile(currentFile);
        } else {
            statusLabel.setText("当前没有可刷新的文件");
        }
    }

    private void toggleTheme() {
        Theme nextTheme = preferences.theme() == Theme.DARK ? Theme.LIGHT : Theme.DARK;
        preferences = preferences.withTheme(nextTheme);
        preferenceService.save(preferences);

        RenderedDocument currentDocument = readerState.getCurrentDocument();
        if (currentDocument != null) {
            String html = htmlTemplateService.buildHtml(currentDocument, preferences);
            webEngine().loadContent(html);
        } else {
            loadWelcomePage();
        }
        statusLabel.setText("主题已切换为: " + nextTheme.name());
    }

    private void adjustFontSize(int delta) {
        int nextSize = Math.max(12, Math.min(28, preferences.fontSize() + delta));
        preferences = preferences.withFontSize(nextSize);
        preferenceService.save(preferences);

        RenderedDocument currentDocument = readerState.getCurrentDocument();
        if (currentDocument != null) {
            String html = htmlTemplateService.buildHtml(currentDocument, preferences);
            webEngine().loadContent(html);
        } else {
            loadWelcomePage();
        }
        statusLabel.setText("字号: " + nextSize);
    }

    public void search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            statusLabel.setText("请输入搜索关键字");
            return;
        }
        boolean found = searchService.find(webEngine(), keyword);
        readerState.setCurrentSearchKeyword(keyword);
        statusLabel.setText(found ? "找到关键字: " + keyword : "未找到: " + keyword);
    }

    private void updateToc(List<TocItem> items) {
        TreeItem<TocItem> root = new TreeItem<>(new TocItem("ROOT", "root", 0, List.of()));
        root.setExpanded(true);
        populateTree(root, items);
        tocTree.setRoot(root);
    }

    private void populateTree(TreeItem<TocItem> parent, List<TocItem> items) {
        for (TocItem item : items) {
            TreeItem<TocItem> treeItem = new TreeItem<>(item);
            treeItem.setExpanded(true);
            parent.getChildren().add(treeItem);
            populateTree(treeItem, item.children());
        }
    }

    private void jumpToAnchor(String anchorId) {
        if (anchorId == null || anchorId.isBlank()) {
            return;
        }
        searchService.jumpToAnchor(webEngine(), anchorId);
    }

    private void loadWelcomePage() {
        TreeItem<TocItem> root = new TreeItem<>(new TocItem("ROOT", "root", 0, List.of()));
        root.setExpanded(true);
        tocTree.setRoot(root);
        String html = htmlTemplateService.buildWelcomePage(preferences);
        webEngine().loadContent(html);
        statusLabel.setText("准备就绪");
    }

    private void selectAnchor(String anchorId) {
        if (anchorId == null || anchorId.isBlank() || tocTree.getRoot() == null) {
            return;
        }
        TreeItem<TocItem> target = findTreeItem(tocTree.getRoot(), anchorId);
        if (target == null) {
            return;
        }

        suppressTocNavigation = true;
        try {
            tocTree.getSelectionModel().select(target);
        } finally {
            suppressTocNavigation = false;
        }
    }

    private TreeItem<TocItem> findTreeItem(TreeItem<TocItem> current, String anchorId) {
        if (current.getValue() != null && anchorId.equals(current.getValue().anchorId())) {
            return current;
        }
        for (TreeItem<TocItem> child : current.getChildren()) {
            TreeItem<TocItem> found = findTreeItem(child, anchorId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("打开失败");
        alert.setHeaderText("Markdown 文件加载失败");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadErrorPage(String message) {
        String html = htmlTemplateService.buildErrorPage(message, preferences);
        webEngine().loadContent(html);
    }

    private WebEngine webEngine() {
        return webView.getEngine();
    }
}
