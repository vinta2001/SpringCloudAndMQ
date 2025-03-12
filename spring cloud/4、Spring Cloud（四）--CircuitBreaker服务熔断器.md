# CircuitBreaker服务熔断
## resilience4j快速入门
[Resilience4j](https://github.com/resilience4j/resilience4j) 是一个轻量级的容错库，提供了诸如熔断、重试、限流、超时、舱壁等功能，可以帮助应用程序在面对故障和不稳定条件时保持可用性和可靠性。
### 参数说明
-  `resilience4j`熔断器参数配置说明

| name                                           | description                                                                                                                                                            |
| ---------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `failure-rate-threshold`                       | 以百分比配置失败率的峰值                                                                                                                                                           |
| `sliding-window-type`                          | 设置断路器的滑动窗口类型，可基于次数和基于时间                                                                                                                                                |
| `sliding-window-size`                          | 如果是基于次数，当`sliding-window-size`次请求中<br>有`百分之failure-rate-threshold`失败时打开断路器，<br>如果是基于时间，当`sliding-window-size`次请求<br>全部发送完成时间超过`slow-call-duration-threshold`<br>时打开熔断器 |
| `slow-call-rate-threshold`                     | 以百分比的方式配置，断路器把调用时间<br>大于`slow-call-duration-threshold`<br>的请求视为慢调用，<br>当慢调用比例大于等于`slow-call-rate-threshold`时<br>打开熔断器，并进入服务降级                                          |
| `slow-call-duration-threshold`                 | 配置调用时间的峰值，当调用时间<br>高于该值时被视为慢调用                                                                                                                                         |
| `permitted-number-of-calls-in-half-open-state` | 运行断路器在`half-open`状态下时进行<br>`permitted-number-of-calls-in-half-open-state`<br>次调用，如果鼓掌或慢速调用仍高于阈值，<br>断路器再次进入打开状态                                                        |
| `minimum-number-of-calls`                      | 在每个滑动窗口期的样本数，配置断路器计算<br>错误率或者慢调用率的最小调用数。比如设置<br>为5意味着在计算故障率之前必须进行至少5次<br>的调用。如果值记录了4次，计时全部失败也不<br>会进入熔断状态                                                              |
| `wait-duration-in-open-state`                  | 从`open`到`half-open`状态需要等待的时间                                                                                                                                           |
### 基于次数的滑动窗口
#### 参数配置
-  导入依赖
由于熔断是客户端需要做的，所以在feign项目中导入aop和resilience4j的maven依赖
```xml
<dependency>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-aop</artifactId>  
</dependency>  
  
<dependency>  
    <groupId>org.springframework.cloud</groupId>  
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>  
    <version>3.1.0</version>  
</dependency>
```
- 修改配置文件
- 打开熔断开关
```yaml
spring:
  cloud:
    openfeign:
	  circuitbreaker:  
	    enabled: true  
		  group:  
		    enabled: true
```
- 熔断器配置
```yaml
resilience4j:  
  circuitbreaker:  
    configs:  
      default:  
        # 失败占比阈值  
        failure-rate-threshold: 50  
        # 滑动窗口类型  
        sliding-window-type: COUNT_BASED  
        # 滑动窗口大小  
        # 基于次数则发送size次请求  
        # 基于时间则发送size秒内的请求，超时视为失败  
        sliding-window-size: 6  
        # 熔断器最小尝试次数  
        # 到达minimum-number次请求后才开始计算故障率  
        minimum-number-of-calls: 6  
        # 半开与全开的自动转换  
        automatic-transition-from-open-to-half-open-enabled: true  
        # 全开到半开的等待时间  
        wait-duration-in-open-state: 5s  
        # 允许半开调用数  
        permitted-number-of-calls-in-half-open-state: 2  
        # 报错类型  
        record-exceptions:  
          - java.lang.Exception  
    # 实例配置  
    instances:  
      # 是例名称  
      pay-service:  
        # 配置选择  
        base-config: default
```
#### 熔断测试
- 微服务接口准备
这里准备一个微服务接口，分别准备多个`Exception`条件。这里需要注意这里的`Exception`不能进入到全局异常处理，否则无法被客户端捕获。
```java
package com.vinta.cloud.controller;  
  
import cn.hutool.core.util.IdUtil;  
import org.springframework.web.bind.annotation.GetMapping;  
import org.springframework.web.bind.annotation.PathVariable;  
import org.springframework.web.bind.annotation.RestController;  
  
import java.util.concurrent.TimeUnit;  
  
@RestController  
public class PayCircuitController {  
  
    @GetMapping("/pay/circuit/{id}")  
    public String myCircuit(@PathVariable("id")Integer id){  
        if(id == -4){  
            throw new RuntimeException("id不能为负数");  
        }        
        if(id==9999){  
            try{  
                TimeUnit.SECONDS.sleep(5);  
            }catch (Exception e){  
                throw new RuntimeException("id不能为9999");  
            }        
        }        
    return "hello,circuit!input:"+id+"\t"+ IdUtil.simpleUUID();  
    }
}
```
- openFeign准备
```java
@GetMapping("/api/pay/circuit/{id}")  
String myCircuitBreaker(@PathVariable("id") Integer id);
```
- 客户端准备
```java
@RestController  
public class OrderFeignController {  
    @Resource  
    private PayFeignAPI payFeignAPI;  
  
    @GetMapping("/feign/pay/circuit/{id}")  
    @CircuitBreaker(name = "pay-service", fallbackMethod = "myCircuitBreaker")  
    public String myCircuitBreaker(@PathVariable("id")Integer id){  
        return payFeignAPI.myCircuitBreaker(id);  
    }  
    
    public String myCircuitBreaker(Integer id, Throwable throwable){  
        return "系统繁忙，请稍后再试~";  
    }
}
```
- feign接口
```java
@GetMapping("/api/pay/circuit/{id}")  
String myCircuitBreaker(@PathVariable("id") Integer id);
```
- 测试结果
随机打开一个api测试工具，访问id=11的url，得到下图所示的结果
![[Pasted image 20240319121230.png]]
然后访问id=-4的url得到失败的结果
![[Pasted image 20240319121320.png]]
如果这里连续访问达到6次，且有3次访问失败，则该客户端会判断微服务发生错误，从而导致服务降级。
![[Pasted image 20240319121548.png]]
### 基于时间的滑动窗口
#### 参数配置
```yaml
resilience4j:  
  timelimiter:  
    configs:  
      default:  
        # 超时时间，有默认值1s，当调用时间超过该值时自动降级或者报错
        timeoutDuration: 1s  
  circuitbreaker:  
    configs:  
      default:  
        # 失败占比阈值  
        failure-rate-threshold: 50  
        # 滑动窗口类型  
        sliding-window-type: TIME_BASED  
        # 滑动窗口大小  
        # 基于次数则发送size次请求  
        # 基于时间则发送size秒内的请求，超时视为失败  
        sliding-window-size: 6  
        # 当调用超过这个duration-threshold时，视为慢调用  
        slow-call-duration-threshold: 2s  
        # 当慢调用超过这个rate-threshold时，熔断器打开  
        slow-call-rate-threshold: 30  
        # 允许在半开状态中的调用数  
        permitted-number-of-calls-in-half-open-state: 2  
        # 半开与全开的自动转换  
        automatic-transition-from-open-to-half-open-enabled: true  
        # 全开到半开的等待时间  
        wait-duration-in-open-state: 5s  
        # 报错类型  
        record-exceptions:  
          - java.lang.Exception  
    # 实例配置  
    instances:  
      # 是例名称  
      pay-service:  
        # 配置选择  
        base-config: default
```
## 关于超时时间的作用的总结
1. OpenFeign中`spring.cloud.openfeign.client.config.default.connectTimeout`是微服务与客户端之间建立连接的时间
2. OpenFeign中`spring.cloud.openfeign.client.config.default.readTimeout`是从客户端请求微服务到返回响应的时间
3. resilience4j中`resilience4j.timelimiter.configs.default.timeoutDuration`也是从客户端请求微服务到返回响应的时间
4. 第2点与第3点没有太多的区别，但是谁数值小谁生效
## Bulkhead舱壁
舱壁是用来限制并发数量，防止微服务访问量过大而造成微服务崩溃使得全部微服务下线

舱壁有两种机制，分别是基于信号量的隔离机制和基于固定线程池隔离机制
### 参数说明
- `maxConcurrentCalls`:允许的最大并行执行数量，默认25
- `maxWaitDuration`:尝试进入饱和舱壁时，应阻塞线程的最长时间，默认0
### 基于信号量
#### 参数配置
-  依赖导入
```xml
<dependency>  
    <groupId>io.github.resilience4j</groupId>  
    <artifactId>resilience4j-bulkhead</artifactId>  
    <version>2.2.0</version>  
</dependency>
```
- 修改配置
```yaml
resilience4j:  
  timelimiter:  
    configs:  
      default:  
        timeoutDuration: 10s  
  bulkhead:  
    configs:  
      default:  
        max-concurrent-calls: 2  
        max-wait-duration: 20ms  
    instances:  
      pay-service:  
        base-config: default
```
#### 熔断测试
- 客户端
```java
    @GetMapping("/feign/pay/bulkhead/{id}")  
    @Bulkhead(name = "pay-service", fallbackMethod = "myBulkhead",type = Bulkhead.Type.SEMAPHORE)  
    public String myBulkhead(@PathVariable("id")Integer id){  
        return payFeignAPI.myCircuitBreaker(id);  
    }  
    public String myBulkhead(Integer id, Throwable throwable){  
        return "系统繁忙，请稍后再试~ from bulkhead";  
    }
}
```
- 微服务
```java
@GetMapping("/pay/bulkhead/{id}")  
public String myBulkhead(@PathVariable("id")Integer id){  
    if(id == -4){  
        throw new RuntimeException("id不能为负数");  
    }    
    if(id==9999){  
        try{  
            TimeUnit.SECONDS.sleep(5);  
        }        catch (Exception e){  
            throw new RuntimeException("id不能为9999");  
        }    
    }    
    return "hello,circuit!input:"+id+"\t"+ IdUtil.simpleUUID();  
}
```
- feign接口
```java
@GetMapping("/api/pay/bulkhead/{id}")  
String myBulkhead(@PathVariable("id") Integer id);
```
### 基于固定线程池舱壁
#### 参数配置
- 舱壁配置
```yaml
resilience4j:  
  timelimiter:  
    configs:  
      default:  
        timeoutDuration: 10s  
  thread-pool-bulkhead:  
    configs:  
      default:  
        # 线程池最大线程数  
        max-thread-pool-size: 1  
        # 线程池核心线程数  
        core-thread-pool-size: 1  
        # 队列容量  
        queue-capacity: 1  
    instances:  
      pay-service:  
        base-config: default
```
- 熔断器取消分组，新线程与主线程分离
```yaml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true
        group: 
          enable: false
```
- 修改客户端
```java
@GetMapping("/feign/pay/bulkhead/{id}")  
@Bulkhead(name = "pay-service", fallbackMethod = "myBulkheadFallback",type = Bulkhead.Type.THREADPOOL)  
public CompletableFuture<String> myBulkhead(@PathVariable("id")Integer id){  
    log.info("in to :"+ Thread.currentThread().getName()+"\t"+DateUtil.now());  
    try{TimeUnit.SECONDS.sleep(5);}catch (Exception e){e.printStackTrace();}  
    log.info("out to :"+ Thread.currentThread().getName()+"\t"+DateUtil.now());  
    return CompletableFuture.supplyAsync(() -> payFeignAPI.myCircuitBreaker(id)+"THREAD-POOL");  
}  
  
public CompletableFuture<String> myBulkheadFallback(Integer id, Throwable throwable){  
    return CompletableFuture.supplyAsync(() -> "系统繁忙，请稍后再试~ from THREAD-POOL");  
}
```
### 熔断测试
这里的熔断测试需要分别发送三个不同的url，才能得到同一个线程池的不同线程
[localhost:9090/api/feign/pay/bulkhead/1](http://localhost:9090/api/feign/pay/bulkhead/1)
[localhost:9090/api/feign/pay/bulkhead/2](http://localhost:9090/api/feign/pay/bulkhead/2)
[localhost:9090/api/feign/pay/bulkhead/3](http://localhost:9090/api/feign/pay/bulkhead/3)
