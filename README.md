#### 项目简介：

本项目基于 Spring AI 与本地部署的 deepseek-r1:8b，开发了一个支持自然语言查询数据库并自动生成统计图表的 AI Agent，实现一句话生成 SQL、执行并返回可视化结果（含执行 SQL 与 base64 编码图片），显著提升数据统计效率。

#### 项目亮点：

- 端到端自动化：将自然语言查询自动转换为 SQL和可执行脚本，生成可视化结果，减少人工干预；
- 可审计输出：前端同时返回并显示所执行的 SQL，保证查询可复核与安全性；
- 多数据库支持：通过 JDBC 无缝支持 MySQL / Oracle / SQL Server，并获取完整表结构信息作为Prompt的一部分；
- 可视化交付：通过进程调用 Python 生成图表并以 base64 返回，兼容前端即时渲染。


#### 界面如下
<img width="2560" height="1288" alt="image" src="https://github.com/user-attachments/assets/7319dc5b-700f-4563-84d5-08b9d9810b50" />
