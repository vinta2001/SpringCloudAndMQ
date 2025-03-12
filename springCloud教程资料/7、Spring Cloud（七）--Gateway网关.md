
## 🤚我的博客

- 欢迎光临我的博客：<a>`https://blog.csdn.net/qq_52434217?type=blog`</a>

## 🥛前言
微服务框架中`gateway`的简单用法。
# 📖Gateway

[Spring Cloud Gateway 官网：https://docs.spring.io/spring-cloud-gateway/reference/4.1-SNAPSHOT/index.html](https://docs.spring.io/spring-cloud-gateway/reference/4.1-SNAPSHOT/index.html)

[Spring Cloud Gateway中文文档：https://springdoc.cn/spring-cloud-gateway/](https://springdoc.cn/spring-cloud-gateway/)

## 💠微服务框架与gateway的作用

Spring Cloud Gateway提供的是一个用于在Spring MVC之上构建API网关的library，它的目标是提供一种简单而有效的方式路由API请求，它提供了一个切面，主要关注：安全、监控/metrics、弹性伸缩。

1. **Spring Cloud** **Gateway****的作用**：
    
    1. **统一****网关****入口**：作为所有微服务的统一入口，实现请求的路由和转发，隐藏了后端服务的细节。
        
    2. **安全认证**：处理认证和授权，实现统一的安全机制，比如鉴权、JWT验证等。
        
    3. **流量控制**：实现流量限制、熔断、重试等机制，保障系统的稳定性和可靠性。
        
    4. **日志记录**：记录请求信息，进行监控和日志输出，方便故障排查和性能优化。
        
    5. **负载均衡**：支持负载均衡策略，实现请求的分发和负载均衡，提高系统的处理能力和可用性。
        

Spring Cloud Gateway作为Spring Cloud生态系统中的重要组件，扮演着统一入口、安全认证、流量控制、日志记录和负载均衡等关键角色，帮助开发人员构建健壮、高可用的微服务架构。其灵活的设计和丰富的功能使得它成为构建现代云原生应用的理想选择。

![](https://niipetoekho.feishu.cn/space/api/box/stream/download/asynccode/?code=ZGMyMWMwMWIwOTcxYjJlMmQyMTdhY2VlNzU4OGIxNTNfV3lMejJWUFNKZEVVSmZXc3lSWmk1OGdyazMxWmVyZ3JfVG9rZW46S3F4bmJTZzNxb3YwU2R4N1RkSWM0c3pmbjdiXzE3MTEwMjQ2Mjk6MTcxMTAyODIyOV9WNA)

## 💠三大概念

**Route：**网关的基本结构块，通过id，目标url和一组谓语（predicate）和过滤器组成。当谓语为true匹配该路由。

**Predicate：**java8的函数谓语。输入类型为Spring Framework ServerWebExchange。这让你匹配HTTP请求中的任何内容，例如标头或参数。

**Filter：**Spring框架中GatewayFilter的实例，使用过滤器可以在请求被路由前或者之后对请求进行修改。

## 💠创建网关服务

### 前置准备

- 导入依赖
    
```XML
<dependencies>

    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-consul-discovery</artifactId>
        <version>4.0.0</version>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>

</dependencies>
```

- 修改配置

```YAML
server:
  port: 9527

spring:
  application:
    name: server-gateway
  cloud:
    consul:
      host: localhost
      port: 8500
      discovery:
        prefer-ip-address: true
        service-name: ${spring.application.name}
```

- 启动类
```Java
package com.vinta.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MainGateway {
    public static void main(String[] args) {
        SpringApplication.run(MainGateway.class, args);
    }
}
```

### 引入gateway配置

```YAML
spring:
  cloud:
    gateway:
      routes:
        - id: pay_service1
          # 改成自己的url
          uri: http://localhost:9091
          predicates:
            - Path=/api/pay/gateway/**
```

现在访问`http://localhost:9527/api/pay/gateway/get/1`即可得到数据

## 💠路由

### 修改注解

- feign接口修改注解
    
```Java
@FeignClient(name = "server-gateway")
public interface PayFeignAPI {
}
```

- 添加api接口
    
```Java
@GetMapping("/pay/gateway/get/{id}")
ResultDTO<PayDTO> getPayWithGateway(@PathVariable("id") String id);
```

配置网关后，关闭网关无法访问
### 动态URL

```YAML
spring:
  cloud:
    gateway:
      routes:
        - id: pay_service1
          uri: lb://pay-service
          predicates:
            - Path=/api/pay/gateway/**
```

## 💠网关谓语

启动gateway项目时会加载谓语工厂，当没有进行配置时会加载默认的谓词。谓词有下面图中几种类型

![](images/Pasted%20image%2020240708212808.png)

在配置谓词时有两种方式，一种是简洁型，一种是完整型。简洁式只需要在predicates下面写上谓词的类型，并用等号连接属性值即可,并且键与值用逗号分隔。前面的配置是简洁式配置。

简洁型的配置如下面所示

```YAML
spring:
  cloud:
     gateway:
       routers:
         - id: cookie_route
           url: https://example.org
           predicates:
           - Cookie=mycooke,mycookievalue
```

完整型配置需要严格遵守键值对的方式。完整型的配置如下所示

```YAML
spring:
  cloud:
     gateway:
       routers:
         - id: cookie_route
           url: https://example.org
           predicates:
           - name: Cooke
             args: 
               name: mycookie
               regexp: mycookieValue
```

### after、before与between谓词

After路由谓词工厂接受一个日期时间，这个日期时间是java的ZonedDateTime类型。该谓词允许在匹配时间后面发送请求。该谓语可以实现定点秒杀与抢购的放行。下面是简单的例子

```YAML
spring:
  cloud:
    gateway:
      routes:
        - id: pay_service1
          uri: lb://pay-service
          predicates:
            - Path=/api/pay/gateway/**
            # 2024-3-20-21:40:55允许访问
            - After=2024-03-20T21:40:55.413946400+08:00[GMT+08:00]
```

获取ZonedDateTime时间

```Java
//当前时间
ZonedDateTime now = ZonedDateTime.now();
// 获取指定时间
ZonedDateTime now = ZonedDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZonedDateTime.now().getZone());
```

这里可以访问9090端口的服务，因为9090访问9091需要经过一个9527的网关。当然也可以直接访问9527这个端口的服务。

![](images/Pasted%20image%2020240708212825.png)

before谓语与after谓语一样，between谓语则多传入一个参数

```YAML
spring:
  cloud:
    gateway:
      routes:
        - id: pay_service1
          uri: lb://pay-service
          predicates:
            - Path=/api/pay/gateway/**
            - Between=2017-01-20T17:42:47.789-07:00[America/Denver], 2017-01-21T17:42:47.789-07:00[America/Denver]
```

### cookie谓语

Cookie 谓语需要两个参数，一个是 Cookie name,一个是正则表达式。

路由规则会通过获取对应的 Cookie name 值和正则表达式去匹配，如果匹配上就会执行路由，如果没有匹配上则不执行

```YAML
spring:
  cloud:
    gateway:
      routes:
        - id: pay_service1
          uri: lb://pay-service
          predicates:
            - Path=/api/pay/gateway/**
            # 2024-3-20-21:40:55允许访问
            - After=2024-03-20T21:40:55.413946400+08:00[GMT+08:00]
            - Cookie=chocolate, ch.p
```

### 自定义谓语

自定义谓语有两种方法，一种是按照官网给出的方法写一个实现RequestPredicate接口的类，然后写一个配置类将自定义谓语注册到网关中。这种方法后面单独研究一下。下面的方法是按照gateway的默认谓语的格式去定义一个谓语，简单高效。

1、新建一个以RoutePredicateFactory类，并继承AbstractRoutePredicateFactory类

2、新建apply方法所需要的内部类config，自定义谓语规则

3、写一个空参构造器调用super

4、重写apply方法

```Java
@Component
public class MyRoutePredicateFactory extends AbstractRoutePredicateFactory<MyRoutePredicateFactory.Config> {

    public MyRoutePredicateFactory() {
        super(MyRoutePredicateFactory.Config.class);
    }

    @Validated
    public static class Config{
        @NotEmpty
        @Getter
        @Setter
        public String userType;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.singletonList("userType");
    }

    @Override
    public Predicate<ServerWebExchange> apply(MyRoutePredicateFactory.Config config) {
        return new Predicate<ServerWebExchange>() {
            @Override
            public boolean test(ServerWebExchange serverWebExchange) {
                String userType = serverWebExchange.getRequest().getQueryParams().getFirst("userType");
                if(userType==null){
                    return false;
                }
                if(userType.equalsIgnoreCase(config.getUserType())){
                    return true;
                }
                return false;
            }
        };
    }
}
```

修改配置文件
```YAML
spring:
  cloud:
    gateway:
      routes:
        - id: pay_service1
          uri: lb://pay-service
          predicates:
            - Path=/api/pay/gateway/**
            - My=diamond
```

测试地址

http://localhost:9527/api/pay/gateway/get/1?userType=diamond

## 💠网关过滤器

网关过滤器可以进行请求鉴权和异常处理

#### 请求头

#### 请求参数

#### 响应

#### 路径

跟路径相关的过滤器可以与谓语相结合使用。

- 路径前缀
我们将路径中的/api/feign这个前缀移动到filters中，然后将predicates中的Path改为。

修改配置
```YAML
spring:
  cloud:
    gateway:
      routes:
        - id: pay_service1
          uri: lb://pay-service
          predicates:
            - Path=/pay/gateway/**
          filters:
            - PrefixPath=/api
```
测试地址
http://localhost:9527/pay/gateway/get/1

- 路径替换

这里用花括号包裹的地方会不会被替换，而其他地方则会被predicates中的Path替换成SetPath的路径

如在下面的例子中`http://localhost:9527/xyz/abc/1`会被替换成`http://localhost:9527/api/pay/gateway/get/1`

```YAML
spring:
  cloud:
    gateway:
      routes:
        - id: pay_service1
          uri: lb://pay-service
          predicates:
            - Path=/xyz/abc/{id}
          filters:
            - SetPath=/api/pay/gateway/get/{id}
```
测试地址
http://localhost:9527/xyz/abc/1
- 重定向
```YAML
spring:
  cloud:
    gateway:
      routes:
        - id: pay_service1
          uri: lb://pay-service
          predicates:
            - Path=/xyz/abc/{id}
          filters:
            - RedirectTo=302,https://www.baidu.com
```
### 自定义过滤器

#### 全局过滤器

按照官方给的例子实现一个自定义全局过滤器，该过滤器可以计算每个接口函数执行的时间

```Java
package com.vinta.cloud.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CustomerGlobalFilter implements GlobalFilter, Ordered {

    public static final String BEGIN="BEGIN_TIME";
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put(BEGIN, System.currentTimeMillis());
        // 这里是一个异步调用方法
        // 当chain.filter(exchange)完成后会自动调用then函数中的参数
        // 也就是Mono.fromRunnable返回的Mono方法
        return chain.filter(exchange).then(Mono.fromRunnable(()->{
            Long begin = exchange.getAttribute(BEGIN);
            if(begin!=null){
                log.info("总耗时：{}ms",System.currentTimeMillis()-begin);
            }
        }));
    }

    /**
     * 数字越小，优先级越高
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
```

#### 条件过滤器

自定义过滤器有两种方法，一种是按照官网给出的方法写一个实现RequestPredicate接口的类，然后写一个配置类将自定义过滤器注册到网关中。这种方法后面单独研究一下。下面的方法是按照gateway的默认过滤器的格式去定义一个过滤器，简单高效。

1、新建一个`RoutePredicateFactory`类，并继承`AbstractRoutePredicateFactory`类

2、新建apply方法所需要的内部类config，自定义过滤器规则

3、写一个空参构造器调用super

4、重写apply方法

```Java
@Component
@Slf4j
public class ConditionGatewayFilterFactory extends AbstractGatewayFilterFactory<ConditionGatewayFilterFactory.Config> {

    public ConditionGatewayFilterFactory(){
        super(ConditionGatewayFilterFactory.Config.class);
    }

    public List<String> shortcutFieldOrder() {
        return Arrays.asList("status");
    }

    @Override
    public GatewayFilter apply(ConditionGatewayFilterFactory.Config config) {
        return (exchange, chain) -> {
            URI uri = exchange.getRequest().getURI();
            if(uri.getRawQuery().contains(config.getStatus())){
                return chain.filter(exchange);
            }
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        };
    }

    @Getter@Setter
    public static class Config{
        private String status;
    }
}
```
## 💠END

至此，本章主要介绍了分布式微服务中网关的基本使用，有问题可以留言。
### 公众号

欢迎关注小夜的公众号，一个立志什么都能会的研究生。
![微信公众号](https://img-blog.csdnimg.cn/img_convert/5a5d07d81cdc0f91842cb784cc75bb39.png#pic_center =300x300)

---