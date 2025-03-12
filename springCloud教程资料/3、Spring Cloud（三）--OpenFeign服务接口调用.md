# OpenFeign
`OpenFeign`是一个声明式web客户端，`OpenFeign`允许开发者们以接口的形式对外暴露服务接口，从而实现面向接口编程。这样做带来的好处是在业务层的实现时不用过多的关注api接口的如何实现，只需要知道有接口的功能，就比如mapper层的`@Mapper`注解。

说人话就是在`controller`层中采用类似于`service`的接口去调用微服务，而不是在`controller`层中采用`restTemplate`用http方法去请求对应的微服务，尽管本质上是采用http请求。
## 快速入门
### 主启动类的注解
使用`OpenFeign`需要在客户端app的主启动类上加上注解`@EnableDiscoveryClient`和`@EnableFeignClients`

```java
@SpringBootApplication  
@EnableDiscoveryClient  
@EnableFeignClients  
public class MainOrderFeign {  
    public static void main(String[] args) {  
        SpringApplication.run(MainOrderFeign.class, args);  
    }}
```
### 接口编写
在此之前，我们已经写过了一个支付微服务。其端口号为`9091`，微服务名称为`pay-service`，模块名称为`pay-service-9091`。提供了pay的增删改查和列表查询的方法。该微服务的请求方法和url如下。这里我们只关注`payController`中有哪些方法。
```java
@RestController  
@Slf4j  
@Tag(name = "支付账单管理")  
@RequestMapping("/pay")  
public class PayController {  
  
    @Resource  
    private PayService payService;  
  
  
    @PostMapping("/add")  
    @Operation(summary = "添加支付账单")  
    public ResultDTO addPay(@RequestBody PayDTO payDTO){  
    }  
    @DeleteMapping("/delete/{id}")  
    @Operation(summary = "删除支付账单")  
    public ResultDTO deletePay(@PathVariable String id){  
    }  
    @PutMapping("/update")  
    @Operation(summary = "更新支付账单")  
    public ResultDTO updatePay(@RequestBody PayDTO payDTO){  
        
    }  
    @GetMapping("/get/{id}")  
    @Operation(summary = "获取支付账单")  
    public ResultDTO getPay(@PathVariable String id){  
         
    }  
    @GetMapping("/list")  
    @Operation(summary = "获取支付账单列表")  
    public ResultDTO<PayPageDTO<Pay>> getPayList(PayPageVO PayPageVO){  
        
    }  
    @Value("${server.port}")  
    private String port;  
    
    @GetMapping("/getSomeInfo")  
    public String getSomeInfo(@Value("${activate.info}") String info){  
        return port + ":" + info;  
    }
}
```
使用OpenFeign可以将这些方法抽象到一个api接口中。这样我们的客户端只需要关注有哪些接口和方法。

下面具体介绍一下整个项目的实现。

首先要将微服务接口写到通用的模块中，以暴露给业务模块中。该接口需要加上`@FeignClient("micro-service-name/with-path")`
```java

@FeignClient(name = "pay-service/api/pay")  
public interface PayFeignAPI {  
  
	@PostMapping("/add")  
	ResultDTO addPay(@RequestBody PayDTO payDTO);  
	@DeleteMapping("/delete/{id}")  
	ResultDTO deletePay(@PathVariable("id") String id);  
	@PutMapping("/update")  
	ResultDTO updatePay(@RequestBody PayDTO payDTO);  
	@GetMapping("/get/{id}")  
	ResultDTO getPay(@PathVariable("id") String id);  
	@GetMapping("/list")  
	ResultDTO<PayPageDTO<Pay>> getPayList(@RequestParam("PayPageVO") PayPageVO PayPageVO);  
	
	@GetMapping("/getSomeInfo")  
	String getSomeInfo();
}
```
然后在客户端controller接口中采用spring注入IOC的方式去调用`PayFeignAPI`中的方法。
```java
  
@RestController  
@RequestMapping("consumer")  
public class OrderController {  
  
    @Resource  
    private PayFeignAPI payFeignAPI;  
  
  
    @PostMapping("/pay/add")  
    public ResultDTO<String> addOrder(@RequestBody PayDTO payDTO) {  
        return (ResultDTO<String>) payFeignAPI.addPay(payDTO);  
    }  
    @GetMapping("/pay/get/{id}")  
    public ResultDTO<Pay> getOrder(@PathVariable String id) {  
        return payFeignAPI.getPay(id);  
    }  
    @PutMapping("/pay/update")  
    public ResultDTO<String> updateOrder(@RequestBody PayDTO payDTO) {  
        return payFeignAPI.updatePay(payDTO);  
    }  
    @DeleteMapping("/pay/delete/{id}")  
    public ResultDTO<String> deleteOrder(@PathVariable String id) {  
        return payFeignAPI.deletePay(id);  
    }  
    @GetMapping("/pay/list")  
    public ResultDTO<PayPageDTO<Pay>> getOrderList(PayPageVO PayPageVO) {  
        return payFeignAPI.getPayList(PayPageVO);  
    }  
    @GetMapping("/getSomeInfo")  
    public String getSomeInfo() {  
        return payFeignAPI.getSomeInfo();  
    }}
```
而我们实际用到的请求则是通过feignClient帮我们转发到PayController中的，所以前面的`PayFeignAPI`中的参数需要与`PayController`中的接口参数对应。如下为`PayController`中的接口。
```java
@RestController  
@Slf4j  
@Tag(name = "支付账单管理")  
@RequestMapping("/pay")  
public class PayController {  
  
    @Resource  
    private PayService payService;  
  
  
    @PostMapping("/add")  
    @Operation(summary = "添加支付账单")  
    public ResultDTO addPay(@RequestBody PayDTO payDTO){  
        payService.add(payDTO);  
        return ResultDTO.ok("ok");  
    }  
    @DeleteMapping("/delete/{id}")  
    @Operation(summary = "删除支付账单")  
    public ResultDTO deletePay(@PathVariable String id){  
        payService.delete(id);  
        return ResultDTO.ok("ok");  
    }  
    @PutMapping("/update")  
    @Operation(summary = "更新支付账单")  
    public ResultDTO updatePay(@RequestBody PayDTO payDTO){  
        Pay pay = new Pay();  
        BeanUtils.copyProperties(payDTO, pay);  
        payService.update(pay);  
        return ResultDTO.ok("ok");  
    }  
    @GetMapping("/get/{id}")  
    @Operation(summary = "获取支付账单")  
    public ResultDTO getPay(@PathVariable String id){  
        Pay pay = payService.getById(id);  
        PayDTO payDTO = new PayDTO();  
        if(pay == null){  
            return ResultDTO.ok(payDTO);  
        }        BeanUtils.copyProperties(pay,payDTO);  
        return ResultDTO.ok(payDTO);  
    }  
    @GetMapping("/list")  
    @Operation(summary = "获取支付账单列表")  
    public ResultDTO<PayPageDTO<Pay>> getPayList(PayPageVO PayPageVO){  
        IPage<Pay> payDTOIPage = payService.findPayListByQuery(PayPageVO);  
        PayPageDTO<Pay> pageDTO = PayPageDTO.<Pay>builder().items(payDTOIPage.getRecords()).total(payDTOIPage.getTotal()).build();  
        return ResultDTO.ok(pageDTO);  
    }  
    @Value("${server.port}")  
    private String port;  
    @GetMapping("/getSomeInfo")  
    public String getSomeInfo(@Value("${activate.info}") String info){  
        return port + ":" + info;  
    }}
```
注：这里在用feign发送`GET`请求时，需要在函数的参数上加上注解`@RequestParam("variableName")`。另外，我在这里给出的例子中的`@PathVariable`注解没有写上对应的名字，有可能会出现bug，最好写上。
## Feign进阶
### 日志打印
feign的日志由日志级别控制，打印日志输出需要在FeignConfig中配置日志级别
- 修改配置类
```java
@Configuration  
public class OpenFeignConfig {  
    @Bean  
    public Retryer retryer() {  
        return Retryer.NEVER_RETRY;  
    }  
    @Bean  
    Logger.Level logger() {  
        return Logger.Level.FULL;  
    }
}
```
- 修改配置文件
配置文件的路径格式为`logging.level.`+API全限定符+`:debug`

如这里为`logging.level.com.vinta.cloud.apis.PayFeignAPI:debug`
```yaml
  
logging:  
  level:  
    com:  
      vinta:  
        cloud:  
          apis:  
            PayFeignAPI: debug
```
### 超时时间控制
OpenFeign的默认超时时间为`60s`，即等待客户端`60s`响应时间。所以如果超时时间不符合要求可以自定义超时时间。

在yaml中配置`connectTimeout`和`readTimeout`。
- `connectTimeout`是防止服务器处理时间过长而造成调用阻塞，即防止连接建立的时间过长而造成阻塞。
- `readTimeout`从连接建立开始计时并在返回响应时间过长时被触发。

全局配置：对全部微服务都生效 
```yaml
spring:  
  application:  
    name: order-service-open-feign  
  cloud:  
    consul:  
      discovery:  
        heartbeat:  
          enabled: true  
      host: localhost  
      port: 8500  
    openfeign:  
      client:  
        config:  
          default:  
            # 单位：ms  
            connectTimeout: 5000  
            readTimeout: 5000
```
指定微服务配置：对指定的微服务生效
```yaml
spring:  
  application:  
    name: order-service-open-feign  
  cloud:  
    consul:  
      discovery:  
        heartbeat:  
          enabled: true  
      host: localhost  
      port: 8500  
    openfeign:  
      client:  
        config:  
          # 自定义配置  
          pay-service:  
            # 单位：ms  
            connectTimeout: 5000  
            readTimeout: 5000
```
当同时存在全局配置和指定配置时，指定配置优先生效

注意：如果前面在`PayFeignAPI`接口的注解中写的微服务包含路径的话，在`openfeign.config`中无法生效。这里需要
### 重试机制
`openFeign`支持请求重试。前面的feign在尝试请求超时之后直接报错是因为重试机制默认为关，重试机制需要自己手动开启。修改`openFeign`的重试配置需要创建`OpenFeign`的配置类修改其`Retryer`的属性。
```java
@Configuration  
public class OpenFeignConfig {  
    @Bean  
    public Retryer retryer() {  
        return new Retryer.Default(1000, 1000, 3);  
    }
}
```
Default中的参数分别为第一次而第二次之间的间隔、最大的重试间隔、最大重试次数。第二次重试开始后，会以init_interval的值的1.5倍成指数增加。即每一次增加到`init_interval*=1`。

### HttpClient优化
- 导入依赖
这里的httpclient5版本需要与spring boot版本对应，否则会报错，所以这里只需要引入hc5的maven坐标而不引入版本号，通过spring boot的父工程自动传递。
```xml
<dependency>  
    <groupId>org.apache.httpcomponents.client5</groupId>  
    <artifactId>httpclient5</artifactId>  
</dependency>  
  
<dependency>  
    <groupId>io.github.openfeign</groupId>  
    <artifactId>feign-hc5</artifactId>  
    <version>13.1</version>  
</dependency>
```
- 修改配置：spring.cloud.openfeign.httpclient.hc5.enabled=true
```yaml
spring:  
  application:  
    name: order-service-open-feign  
  cloud:  
    consul:  
      discovery:  
        heartbeat:  
          enabled: true  
      host: localhost  
      port: 8500  
    openfeign:  
      client:  
        config:  
          pay-service:  
            # 单位：ms  
            connectTimeout: 3000  
            readTimeout: 1000  
      httpclient:  
        hc5:  
          enabled: true
```
### 请求回应压缩
可以对请求和响应可以实现GZIP压缩，以减少通信过程中的性能损耗。
- 开关配置
```yaml
spring:  
  cloud:  
    openfeign:
	  compression:   
	    request:  
		  enabled: true  
	    response:  
		  enabled: true  
```
- 细粒度配置
```yaml
spring:  
  cloud:  
    openfeign:
	  compression:   
	    request:  
		  enabled: true  
	    response:  
		  enabled: true  
			# 触发压缩的mime类型  
			mime-types: application/json, application/xml, text/xml, text/html, text/plain  
			# 触发压缩的最小阈值  
			min-request-size: 1024
```