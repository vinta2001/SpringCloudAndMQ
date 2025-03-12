# Consul服务注册与发现
## Get Started
-  下载
-  检验
```shell
consul --version
```
-  启动
```shell
consul agent -dev
```
-  访问
[http://localhost:8500]([http://localhost:8500])

## 在Spring cloud中使用Consul
[Spring Cloud Consul](https://docs.spring.io/spring-cloud-consul/reference/index.html)

### 前置准备
#### 配置
- 导入依赖
```xml
<dependency>  
    <groupId>org.springframework.cloud</groupId>  
    <artifactId>spring-cloud-starter-consul-discovery</artifactId>  
    <version>4.0.0</version>  
</dependency>
```
- 配置yaml
```yaml
spring:  
  application:  
    name: par-service
  cloud:  
	consul:  
	host: localhost  
    port: 8500  
    discovery:  
      service-name: ${spring.application.name}
```
-  给启动类加上注解`@EnableDiscoveryClient`
```java
package com.vinta.cloud;  
  
import org.springframework.boot.SpringApplication;  
import org.springframework.boot.autoconfigure.SpringBootApplication;  
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;  
  
@SpringBootApplication  
@EnableDiscoveryClient  
public class MainPay {  
    public static void main(String[] args) {  
        SpringApplication.run(MainPay.class, args);  
    }
}
```
#### 安装中的错误
- 版本不匹配
版本不匹配会导致一些奇奇怪怪的bug，如下面的错误
```shell
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryConfiguration$EmbeddedTomcat': org/springframework/web/client/RestClient$Builder
```
我选择的spring boot版本是`3.0.5`，cloud依赖版本是`2023.0.0`，找了很多方法，最后将consul的版本`4.1.0`改为`4.0.0`才得以解决该bug。
- 心跳不健康
如果启动服务之后一直不健康，则需要在`application.yaml`配置中开启心跳

```yaml
spring: 
  cloud:  
	consul:  
	  host: localhost  
	  port: 8500  
	  discovery:  
		prefer-ip-address: true
		service-name: ${spring.application.name}  
		  # 开启心跳
	 heartbeat:  
		enabled: true
```
`prefer-ip-address`设置为`true`时，在consul管理界面中的微服务会以ip地址的形式存入到管理中心而不是电脑主机名字。
![[Pasted image 20240318233501.png]]
## 动态调用微服务
动态调用微服务是指在controller中调用consul中注册的微服务名作为请求地址。需要注意，由于consul自带的负载均衡属性，会默认一个微服务名称后面有多个ip地址，所以这里需要在`restTemplate`上面加上负载均衡`@LoadBalanced`注解，否则会报错。
```java
package com.vinta.cloud.config;  
  
import org.springframework.cloud.client.loadbalancer.LoadBalanced;  
import org.springframework.context.annotation.Bean;  
import org.springframework.context.annotation.Configuration;  
import org.springframework.web.client.RestTemplate;  
  
@Configuration  
public class RestConfig {  
    @Bean  
    @LoadBalanced    
    public RestTemplate restTemplate() {  
        return new RestTemplate();  
    }
}
```
另外要注意的是，注册的服务名只是替代了`ip:port`ip和端口号，如果你的项目还配置了根路径比如`http://localhost:9090/api`时，需要自己加上`/api`这个路径。

在controller中修改转发url
```java
public static final String PATMENT_URL = "http://pay-service/api";//pay-service为consul中心注册的微服务名称
```

## 服务中心分布式`K\V`配置
为了配合微服务集群的服务，需要将系统的配置信息放入到consul中心。最简单的例子就是每一个微服务都连接到同一个服务器的数据库，当数据库服务器ip变更时，那么变更微服务的url是相当麻烦的。所以需要将一些通用配置信息放到consul中心中。
### 导入依赖
在微服务service中导入依赖
```xml
<dependency>  
    <groupId>org.springframework.cloud</groupId>  
    <artifactId>spring-cloud-starter-consul-config</artifactId>  
    <version>4.0.0</version>  
</dependency>  
  
<dependency>  
    <groupId>org.springframework.cloud</groupId>  
    <artifactId>spring-cloud-starter-bootstrap</artifactId>  
    <version>4.0.0</version>  
</dependency>
```
### 新建yaml配置文件
新建一个`bootstrap.yaml`文件，该配置文件时系统级配置，级别高于`application.yaml`。在新建的`bootstrap.yaml`中要放入一些与微服务相关的配置信息，同时要指定consul中心的文件名分隔符和文件格式。
```yaml
spring:  
  application:  
    name: pay-service  
  cloud:  
    consul:  
      host: localhost  
      port: 8500  
      discovery:  
        service-name: ${spring.application.name}  
        heartbeat:  
          enabled: true  
      config:  
        profile-separator: '-'  
        format: YAML
```
### consul中心配置公共信息
在consul网站上配置`K/V`信息，需要按照下面路径格式配置信息。不同的开发环境会根据application的配置获取不同环境的consul公共信息。

	config/mircoservice-name/data
	config/mircoservice-name-dev/data
	config/mircoservice-name-prod/data

![[Pasted image 20240317210721.png]]
这里，我们在data文件中写下测试信息
```yaml
activate: 
 info: welcome to my site defualt config,version=1
```
![[Pasted image 20240317211355.png]]
### 测试consul信息
 - 测试接口
```java
@Value("${server.port}")  
private String port;  
@GetMapping("/getSomeInfo")  
public String getSomeInfo(@Value("${activate.info}") String info){  
    return port + ":" + info;  
}
```
- 结果
![[Pasted image 20240317212428.png]]
### 服务中心数据动态刷新
实现服务器中更新数据后，客户端访问可以即刻获取最新数据
#### 添加启动程序注解
```java
@SpringBootApplication  
@EnableDiscoveryClient  
@RefreshScope  
public class MainPay {  
    public static void main(String[] args) {  
        SpringApplication.run(MainPay.class, args);  
    }}
```
#### 修改监控时间
```yaml
spring:  
  application:  
    name: pay-service  
  cloud:  
    consul:  
      host: localhost  
      port: 8500  
      discovery:  
        service-name: ${spring.application.name}  
        heartbeat:  
          enabled: true  
      config:  
        profile-separator: '-'  
        format: YAML
        # 监控时间  
        watch:  
          wait-time: 10
```
## consul配置持久化
### consul数据持久化
在consul目录中新建一个`mydata`文件夹，后续的数据会存到该文件夹中
### 注册Windows服务
新建一个`consul_boot.bat`程序

编写内容
```shell
@echo.服务启动
@echo off
@sc create Consul binpath= "D:\Development_Tools\consul_1.18.0_windows_386\consul.exe agent -server -ui -bind=127.0.0.1 -client=0.0.0.0 -bootstrap-expect 1 -data-dir D:\Development_Tools\consul_1.18.0_windows_386\mydata"
@net start Consul
@sc config Consul start= AUTO
@echo.Consul start is OK......success
@pause
```