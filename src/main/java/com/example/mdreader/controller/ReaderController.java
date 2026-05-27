package com.example.mdreader.controller;

import com.example.mdreader.model.DocumentSource;
import com.example.mdreader.model.ReaderPreferences;
import com.example.mdreader.model.ReaderState;
import com.example.mdreader.model.RenderedDocument;
import com.example.mdreader.model.Theme;
import com.example.mdreader.service.FileManager;
import com.example.mdreader.service.HtmlTemplateService;
import com.example.mdreader.service.MarkdownService;
import com.example.mdreader.service.PreferenceService;
import com.example.mdreader.service.SearchService;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
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

    private WebView webView;
    private Label statusLabel;
    private TextField searchField;
    private MenuButton recentFilesButton;
    private Path currentHtmlPreviewFile;

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

        recentFilesButton = new MenuButton("最近");
        rebuildRecentFilesMenu();

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
                recentFilesButton,
                reloadButton,
                themeButton,
                smallerFontButton,
                largerFontButton,
                new Label("搜索"),
                searchBox
        );
    }

    private WebView buildCenter() {
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
        return webView;
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
            loadHtmlDocument(html);
            preferences = preferenceService.rememberFile(preferences, path);
            preferenceService.save(preferences);
            rebuildRecentFilesMenu();
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
            loadHtmlDocument(html);
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
            loadHtmlDocument(html);
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

    private void loadWelcomePage() {
        String html = htmlTemplateService.buildWelcomePage(preferences);
        loadHtmlDocument(html);
        statusLabel.setText("准备就绪");
    }

    private void rebuildRecentFilesMenu() {
        if (recentFilesButton == null) {
            return;
        }

        recentFilesButton.getItems().clear();
        List<String> recentFiles = preferences.recentFiles();
        if (recentFiles.isEmpty()) {
            MenuItem emptyItem = new MenuItem("暂无记录");
            emptyItem.setDisable(true);
            recentFilesButton.getItems().add(emptyItem);
            return;
        }

        for (String filePath : recentFiles) {
            Path path = Path.of(filePath);
            MenuItem item = new MenuItem(path.getFileName().toString());
            item.setOnAction(event -> openRecentFile(path));
            recentFilesButton.getItems().add(item);
        }

        recentFilesButton.getItems().add(new SeparatorMenuItem());
        MenuItem clearItem = new MenuItem("清空记录");
        clearItem.setOnAction(event -> {
            preferences = preferences.withRecentFiles(List.of());
            preferenceService.save(preferences);
            rebuildRecentFilesMenu();
            statusLabel.setText("已清空最近打开记录");
        });
        recentFilesButton.getItems().add(clearItem);
    }

    private void openRecentFile(Path path) {
        if (!path.toFile().exists()) {
            preferences = preferenceService.removeRecentFile(preferences, path);
            preferenceService.save(preferences);
            rebuildRecentFilesMenu();
            statusLabel.setText("最近文件不存在，已移除记录");
            showError("文件不存在: " + path);
            return;
        }
        openFile(path);
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
        loadHtmlDocument(html);
    }

    private WebEngine webEngine() {
        return webView.getEngine();
    }

    private void loadHtmlDocument(String html) {
        try {
            Path nextFile = Files.createTempFile("md-reader-", ".html");
            Files.writeString(nextFile, html, StandardCharsets.UTF_8);
            nextFile.toFile().deleteOnExit();
            deletePreviousPreviewFile();
            currentHtmlPreviewFile = nextFile;
            webEngine().load(nextFile.toUri().toString());
        } catch (IOException ex) {
            throw new IllegalStateException("写入预览页面失败", ex);
        }
    }

    private void deletePreviousPreviewFile() {
        if (currentHtmlPreviewFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(currentHtmlPreviewFile);
        } catch (IOException ignored) {
            // Temporary preview files are best-effort cleanup only.
        }
    }
}
