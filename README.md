# 🎓 学汉字 LearnChinese

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![API](https://img.shields.io/badge/API-29%2B-brightgreen.svg)
![Java](https://img.shields.io/badge/Language-Java%208-orange.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

**✨ 一款简洁优雅的 Android 汉字学习应用 ✨**

[功能特性](#-功能特性) • [界面预览](#-界面预览) • [快速开始](#-快速开始) • [技术栈](#️-技术栈)

</div>

---

## 📖 项目简介

**学汉字** 是一款专为汉字学习者设计的 Android 应用，内置 **9000+ 常用汉字库**，支持搜索、标记、随机抽背等多种学习方式。无论你是汉语初学者还是进阶学习者，都能帮助你高效掌握汉字！

> 🎯 **适合人群**：汉语学习者 · 小学生 · 华文教育 · 汉字爱好者

---

## ✨ 功能特性

### 🔍 智能搜索
- 输入汉字即时显示大号字体，方便临摹学习
- 显示汉字在字库中的编号位置
- 支持实时搜索和跳转

### ⭐ 标记管理
- 一键标记重点汉字，打造个人学习清单
- 标记状态实时保存，不怕丢失
- 支持批量标记/取消标记操作

### 📚 字库浏览
- 网格展示全部汉字，一目了然
- 支持触摸拖动多选
- 快速跳转到字库末尾
- 长按删除不需要的汉字

### 🎲 随机抽背
- 一键随机抽取汉字，测验学习效果
- 支持全库随机或标记汉字随机

### 📤 导出分享
- 导出字库为文本文件
- 支持通过社交应用分享
- 文件名自动带时间戳

### ➕ 自定义字库
- 添加新汉字到字库
- 自动保存用户添加的内容
- 数据持久化存储

---

## 📱 界面预览

| 主界面 | 字库浏览 | 批量操作 |
|:---:|:---:|:---:|
| 🔍 搜索汉字 | 📖 网格展示 | ✅ 多选操作 |
| ⭐ 标记功能 | 🟢 标记状态 | 🗑️ 批量删除 |

---

## 🚀 快速开始

### 环境要求

- Android Studio Hedgehog 或更高版本
- Android SDK 34
- JDK 8 或更高版本
- Android 设备 (API 29+ / Android 10+)

### 安装步骤

```bash
# 1. 克隆项目
git clone https://github.com/your-username/LearnChinese.git

# 2. 用 Android Studio 打开项目
# File → Open → 选择 LearnChinese 文件夹

# 3. 等待 Gradle 同步完成

# 4. 连接 Android 设备或启动模拟器

# 5. 点击 Run 按钮安装应用
```

### 直接下载

前往 [Releases](../../releases) 页面下载最新的 APK 文件直接安装。

---

## 🛠️ 技术栈

| 技术 | 说明 |
|:---|:---|
| ☕ Java 8 | 主要开发语言 |
| 🎨 Material Design | UI 设计规范 |
| 📐 ConstraintLayout | 响应式布局 |
| 💾 SharedPreferences | 轻量数据存储 |
| 📁 FileProvider | 安全文件分享 |

---

## 📁 项目结构

```
LearnChinese/
├── app/
│   └── src/main/
│       ├── java/com/wayne/learnchinese/
│       │   ├── MainActivity.java          # 主界面
│       │   ├── LibraryViewActivity.java   # 字库浏览
│       │   └── CharacterLibrary.java      # 核心数据管理
│       ├── res/
│       │   ├── layout/                    # 布局文件
│       │   ├── values/                    # 资源文件
│       │   └── assets/
│       │       └── chinese.txt            # 9000+ 汉字库
│       └── AndroidManifest.xml
├── build.gradle
└── README.md
```

---

## 🎯 核心类说明

### `CharacterLibrary` - 字库管理核心

```java
// 获取单例实例
CharacterLibrary library = CharacterLibrary.getInstance(context);

// 搜索汉字
int index = library.getCharacterIndex('汉');

// 标记/取消标记
library.toggleStar('汉', true);

// 添加新汉字
library.addCharacter("新汉字");

// 导出字库
library.exportLibrary(context);
```

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

## 📝 更新日志

### v1.0.0
- ✅ 基础汉字库 (9000+ 汉字)
- ✅ 搜索与显示功能
- ✅ 标记管理
- ✅ 字库浏览
- ✅ 随机抽背
- ✅ 导出分享

---

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

## 🌟 致谢

- 汉字库来源：常用汉字表
- 图标：Google Material Icons
- 设计灵感：Material Design 指南

---

<div align="center">

**如果这个项目对你有帮助，请给一个 ⭐ Star 支持一下！**

Made with ❤️ by Wayne

</div>
