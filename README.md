# Easy MD Reader

`Easy MD Reader` 是一款面向 **macOS** 的轻量级 Markdown (`.md`) 阅读器，使用 **Java + JavaFX** 实现，专注于本地 Markdown 文件的快速打开、目录导航和舒适阅读。

## 项目定位

这个项目不是重度编辑器，也不追求笔记管理、插件系统或协同能力，而是聚焦于：

- 快速打开本地 Markdown 文件
- 清晰展示标题、段落、表格、代码块、图片等内容
- 提供目录导航、最近文件、主题切换、字号调整等阅读体验

## 当前功能

- 打开本地 `.md` / `.markdown` / `.mdown` 文件
- 拖拽 Markdown 文件到窗口中直接打开
- HTML 内置目录导航，支持跳转到对应章节
- 支持相对 Markdown 链接跳转
- 当前文档内关键字搜索
- 浅色 / 深色主题切换
- 字号缩放
- 最近打开文件菜单

## 技术栈

- `Java 21+`
- `JavaFX`
- `flexmark-java`
- `Jackson`
- `jpackage`（用于 macOS 打包）

## 本地运行

### 方式一：在 IntelliJ IDEA 中运行

建议运行入口类：

`com.example.mdreader.Main`

### 方式二：使用 Maven

```bash
mvn javafx:run
```

## 项目结构

```text
src/main/java/com/example/mdreader
├── App.java
├── Main.java
├── controller/
├── model/
├── service/
└── util/

src/main/resources
├── css/
├── html/
└── js/
```

## macOS 打包

项目已经附带打包脚本：

```bash
./scripts/package-macos.sh
```

脚本会完成以下步骤：

1. 使用 Maven 编译项目
2. 拷贝运行所需依赖
3. 使用 `jpackage` 生成 macOS 应用
4. 输出 `.app` 和 `.dmg` 安装包

默认产物目录：

```text
dist/
```

## 开发建议

推荐使用：

- `JDK 21`
- `Maven 3.9+`

虽然更高版本 JDK 有时也能运行，但 **JavaFX 与 WebView 在新 JDK 上可能存在兼容性差异**，为了稳定性，建议优先使用 `JDK 21`。

## 示例文件

项目内提供了简单测试文档：

- [examples/sample.md](./examples/sample.md)
- [examples/second.md](./examples/second.md)

可以直接用来验证目录跳转、相对链接、表格和代码块渲染。
