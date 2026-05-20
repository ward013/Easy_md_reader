# Markdown 阅读器示例

这是 `md-reader` 的本地测试样例。

## 功能覆盖

- 标题目录
- 代码块
- 表格
- 引用
- 相对链接

## 代码块

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("hello md-reader");
    }
}
```

## 表格

| 功能 | 状态 | 说明 |
| --- | --- | --- |
| 文件打开 | 已完成 | 支持 `.md` / `.markdown` / `.mdown` |
| 目录导航 | 进行中 | 当前已生成目录树 |
| 搜索 | 基础版 | 当前文档内查找 |

## 引用

> 这是一个面向阅读体验的轻量 Markdown 阅读器。

## 相对链接

[打开第二个文档](./second.md)

## 结尾

继续推进下一阶段：样式外置、链接拦截优化、最近文件菜单。
