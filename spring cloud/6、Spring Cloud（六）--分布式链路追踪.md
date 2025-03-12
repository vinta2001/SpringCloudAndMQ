## 🤚我的博客

- 欢迎光临我的博客：<a>`https://blog.csdn.net/qq_52434217?type=blog`</a>

## 🥛前言
微服务框架中`CircuitBreaker`的简单用法。

[micrometer官网|https://docs.micrometer.io/micrometer/reference/installing.html](https://docs.micrometer.io/micrometer/reference/installing.html)

# 📖Micrometer

## 💠分布式链路追踪

在微服务框架中，一个由客户端发起的请求在后端系统中会经过多个不同的的服务节点调用来协同产生最后的请求结果，每一个前段请求都会形成一条复杂的分布式服务调用链路，链路中的任何一环出现高延时或错误都会引起整个请求最后的失败。

### 概述

链路追踪是分布式系统下的一个概念，它的目的就是要解决上面所提出的问题，也就是将一次分布式请求还原成调用链路，将一次分布式请求的调用情况集中展示，比如，各个服务节点上的耗时、请求具体到达哪台机器上、每个服务节点的请求状态等等。

它主要的作用如下：

1、自动采取数据

2、分析数据，产生完整调用链：有了请求的完整调用链，问题有很大概率可复现

3、数据可视化：每个组件的性能可视化，能帮助我们很好地定位系统的瓶颈，及时找出问题所在

通过分布式追踪系统，我们能很好地定位请求的每条具体请求链路，从而轻易地实现请求链路追踪，进而定位和分析每个模块的性能瓶颈。

### 链路追踪原理

分布式链路的数据模型有以下几个：

1、traceID：单个完整链路的唯一 `id`，客户端请求微服务1，微服务1请求微服务2拥有同一个`traceID`

2、spanID：每一个请求链路的`id`，客户端请求微服务1，微服务1请求微服务2都会有一个`spanID`

3、 parentID:接收到的请求来源id，客户端请求微服务1，则微服务1的`parentID`是客户端id，客户端的`parentID`为`null`

![](https://niipetoekho.feishu.cn/space/api/box/stream/download/asynccode/?code=N2JlM2Y5NDRjYzE4MzMwZjcwZTU2ZDFkMDEzOWJhNjJfbW9FVXRmUFh1akkya3lXM1pLdDV3akhOTk9hN1U1Vm5fVG9rZW46SDVWcmIwUkdXb3RaNXl4em1BSWNUakpBbjJmXzE3MTA5MTk4MjA6MTcxMDkyMzQyMF9WNA)

此图中，

service1接受到网页的请求，`traceId=x`，`spanId=null`，`parentId=null`；

service2接受到service1请求，`traceId=x`，`spanId=B`，`parentId=service1`；

service3接受到service2请求，`traceId=x`，`spanId=C`，`parentId=service2`；

**性能指标**

性能指标是通过发送请求的时间和接受请求的时间差计算得出的，由`serverSentTime`-`clientReceivedTime`可以得到时间差。

## 💠ZIPKIN

**点击直达**：[zipkin官网|https://zipkin.io/](https://zipkin.io/)

Zipkin 是一个开源的分布式跟踪系统。Zipkin可准确确定到应用程序的请求在哪里花费了更多时间。无论是代码内部的调用，还是对另一服务的内部或外部API调用，都可以使用zipkin对系统进行检测以共享上下文。

举个例子，如果微服务应用的响应时间过长，您就可以使用Zipkin了解应用程序是否花费了大部分时间来查询数据库。通过 Zipkin 提供的请求延迟时间可视化，能够发现导致请求时间过长的原因，是由于高速缓存服务器已关闭，故所有调用都直接发送到数据库，从而增加了微服务的延迟。

### 下载

截止到2024-3-20的最新版本下载地址[latest release](https://search.maven.org/remote_content?g=io.zipkin&a=zipkin-server&v=LATEST&c=exec)

![](https://niipetoekho.feishu.cn/space/api/box/stream/download/asynccode/?code=M2ViMTEwNTM0NDFkMjcyMzUyNjc3NWExMGI3YTQzMDZfbVg2TnJNN3Y0dDdjVTAwTGVvTDhySW1KZXE5M2dJSkdfVG9rZW46QTBOa2JpTEJxb3hYa0V4TE85ZGNRcXVlbkplXzE3MTA5MTk4MjA6MTcxMDkyMzQyMF9WNA)

下载完毕后进入到zipkin目录，打开cmd运行jar包，这里替换成实际下载包名

```Shell
java -jar zipkin-server-3.1.1-exec.jar
```

### 父工程依赖

- **导入依赖**
    

```XML
<dependencyManagement>
    <dependencies>
        <!--        micrometer-tracing-bom导入链路追踪版本中心-->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bom</artifactId>
            <version>1.2.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!--        指标追踪-->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing</artifactId>
            <version>1.2.0</version>
        </dependency>

        <!--        适配zipkin的桥接包-->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bridge-brave</artifactId>
            <version>1.2.0</version>
        </dependency>
        <!--        收集应用程序的度量数据-->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-observation</artifactId>
            <version>1.12.0</version>
        </dependency>
        <!--        feign-micrometer 5-->
        <dependency>
            <groupId>io.github.openfeign</groupId>
            <artifactId>feign-micrometer</artifactId>
            <version>13.1</version>
        </dependency>

        <dependency>
            <groupId>io.zipkin.reporter2</groupId>
            <artifactId>zipkin-reporter-brave</artifactId>
            <version>2.17.0</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

- **依赖说明**
    

### 子工程依赖

对所有的微服务都加入依赖，包括客户端项目

- **依赖导入**
    

```XML
<!--        指标追踪-->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing</artifactId>
    <version>1.2.0</version>
</dependency>

<!--        适配zipkin的桥接包-->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
    <version>1.2.0</version>
</dependency>
<!--        收集应用程序的度量数据-->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-observation</artifactId>
    <version>1.12.0</version>
</dependency>
<!--        feign-micrometer 5-->
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-micrometer</artifactId>
    <version>12.5</version>
</dependency>
<!--    zipkin的brave库支持  -->
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
    <version>2.17.0</version>
</dependency>
```

### 参数配置

在微服务的application.yaml中设置如下配置

```YAML
management:
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
  tracing:
    sampling: 
      probability: 1.0 # 采样率，0.1表示10次采样1次
```

### 测试结果

测试url:[http://localhost:9090/api/feign/pay/micrometer/2](http://localhost:9090/api/feign/pay/micrometer/2)

![](https://niipetoekho.feishu.cn/space/api/box/stream/download/asynccode/?code=MjFhM2Y3YTJlNjc5YzEzNjkyM2ZiMjA3NjAwNDU3NTZfSnl0anZjVWg1MmtuR29CY2FEWDRUcEhNTEtZR3diMUtfVG9rZW46SVBVQmJzME96b2U3cUp4eTd1Z2NwNmhnbmtiXzE3MTA5MTk4MjA6MTcxMDkyMzQyMF9WNA)

**路径追踪**

![](https://niipetoekho.feishu.cn/space/api/box/stream/download/asynccode/?code=MjlmOTI0NTBjZDU3MjIxOGJiZTkzZDZhNmEzM2QzZjhfWmdBd3M5Yjh0YU1HdHBLZktpNW10bVRGRURVNlFyZUVfVG9rZW46TmxjWWJxczF3b1JFMzZ4TElicGNFUXBYbnRmXzE3MTA5MTk4MjA6MTcxMDkyMzQyMF9WNA)

**依赖关系**

![](https://niipetoekho.feishu.cn/space/api/box/stream/download/asynccode/?code=ZjM2ZTc1OGRhMDY3MzVhODZjMDM1ZmRmZjI0Y2NkY2ZfZVptOVBtb3hTZEhLNW4zRWRIZGg2aFNxeVVNWmdnT1hfVG9rZW46TnNra2JIb01rb2M1UFJ4SnRKZ2NzSHBobnNoXzE3MTA5MTk4MjA6MTcxMDkyMzQyMF9WNA)

## 💠END

至此，本章主要介绍了分布式微服务中链路追踪的基本使用，有问题可以留言。

### 公众号

欢迎关注小夜的公众号，一个立志什么都能会的研究生。

碎碎念：想学的东西好多哇，分布式微服务打算后面找个项目做一下，不过项目不会写成文章，最多把项目中的一些小技巧总结出来。最近其实已经开始复习机器学习和redis了，后面会继续更新redis的使用和数据分析与机器学习。一起加油! 🆙🆙🆙
![微信公众号](https://img-blog.csdnimg.cn/img_convert/5a5d07d81cdc0f91842cb784cc75bb39.png#pic_center =300x300)

---