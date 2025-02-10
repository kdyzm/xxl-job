<p align="center" >
    <img src="https://www.xuxueli.com/doc/static/xxl-job/images/xxl-logo.jpg" width="150">
    <h3 align="center">XXL-JOB-WEBSOCKET</h3>
    <p align="center">
       WebSocket版本的xxl-job-admin
        <br>
        <a href="https://www.xuxueli.com/xxl-job/"><strong>-- Home Page --</strong></a>
        <br>
        <br>
        <a href="https://github.com/xuxueli/xxl-job/actions">
            <img src="https://github.com/xuxueli/xxl-job/workflows/Java%20CI/badge.svg" >
        </a>
        <a href="https://maven-badges.herokuapp.com/maven-central/com.xuxueli/xxl-job/">
            <img src="https://maven-badges.herokuapp.com/maven-central/com.xuxueli/xxl-job/badge.svg" >
        </a>
        <a href="https://github.com/xuxueli/xxl-job/releases">
         <img src="https://img.shields.io/github/release/xuxueli/xxl-job.svg" >
        </a>
        <a href="https://github.com/xuxueli/xxl-job/">
            <img src="https://img.shields.io/github/stars/xuxueli/xxl-job" >
        </a>
        <a href="https://hub.docker.com/r/xuxueli/xxl-job-admin/">
            <img src="https://img.shields.io/docker/pulls/xuxueli/xxl-job-admin" >
        </a>
        <a href="http://www.gnu.org/licenses/gpl-3.0.html">
         <img src="https://img.shields.io/badge/license-GPLv3-blue.svg" >
        </a>
        <a href="https://www.xuxueli.com/page/donate.html">
           <img src="https://img.shields.io/badge/%24-donate-ff69b4.svg?style=flat" >
        </a>
    </p>
</p>



## Introduction

本分支是改造过的websocket版本的分支，需要搭配xxljob-websocket-spring-boot-starter组件使用，我的改造博文：[爆改xxl-job：websocket版本的改造方案](https://blog.kdyzm.cn/post/285)

地址：https://gitee.com/kdyzm/xxljob-spring-boot-starter

GAV信息：

``` xml
<dependency>
    <groupId>cn.kdyzm</groupId>
    <artifactId>xxljob-websocket-spring-boot-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

本项目整体作为xxl-job-admin使用。

先初始化数据库，运行`https://github.com/kdyzm/xxl-job/blob/websocket/doc/db/tables_xxl_job.sql`数据库脚本。

初始化数据库成功以后，就可以启动xxl-job-admin服务了，可以使用我打包好的docker镜像快速体验，使用`docker-compose up -d`命令运行以下`docker-compose.yaml`文件（注意修改yaml文件中的内容）

```yaml
version: '2'
services:
  xxl-job:
    image: registry.cn-hangzhou.aliyuncs.com/kdyzm/xxl-job-websocket-admin:1.0.1
    restart: always
    container_name: xxl-job-websocket-admin
    environment:
      - SPRING_PROFILE_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:mysql://数据库地址:端口号/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
      - SPRING_DATASOURCE_USERNAME=数据库账号
      - SPRING_DATASOURCE_PASSWORD=数据库密码
      - SPRING_MAIL_HOST=smtp.163.com
      - SPRING_MAIL_PORT=465
      - SPRING_MAIL_USERNAME=邮箱账号
      - SPRING_MAIL_FROM=邮箱账号
      - SPRING_MAIL_PASSWORD=邮箱密码
      - SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
      - SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
      - SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED=true
      - SPRING_MAIL_PROPERTIES_MAIL_SMTP_SOCKETFACTORY_CLASS=javax.net.ssl.SSLSocketFactory
    ports:
      - 8083:8080
```

也可以下载源码：https://github.com/kdyzm/xxl-job 运行xxl-job-admin自行体验下。

服务端启动之后，浏览器访问地址：http://localhost:8080/xxl-job-admin/ 进入管理页面，登录账号密码：admin/admin123

