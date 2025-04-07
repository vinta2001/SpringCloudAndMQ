# 安装
```Shell
docker run \
 -e RABBITMQ_DEFAULT_USER=itheima \
 -e RABBITMQ_DEFAULT_PASS=123321 \
 -v mq-plugins:/plugins \
 --name mq \
 --hostname mq \
 -p 15672:15672 \
 -p 5672:5672 \
 -d \
 rabbitmq:3.8-management
```
可以看到在安装命令中有两个映射的端口：

- 15672：RabbitMQ提供的管理控制台的端口

- 5672：RabbitMQ的消息发送处理接口
# Rabbit MQ
## 交换机
交换机有以下几种，分别推送到不同的队列中。如direct定向推送到某个队列，fanout则广播到所有队列。
![](images/Pasted%20image%2020240720134120.png)
## 队列
队列存放发布过的服务，等待消费者监听
![](images/Pasted%20image%2020240720134740.png)
- 交换机与队列绑定
交换机需要绑定队列，才能进行服务发布
![](images/Pasted%20image%2020240720135014.png)
### Admin
#### user
![](images/Pasted%20image%2020240720135432.png)
#### 虚拟主机
![](images/Pasted%20image%2020240720135620.png)
# Spring-AMQP
Spring AMQP 是 Spring 框架提供的一个基于 AMQP 协议的消息队列框架，用于简化 Spring 应用程序对消息队列的使用。它难点在于减少了对 AMQP 协议的细节处理，提供了一个高级别的抽象，使得生产者和消费者可以用简单的方式与消息队列进行通信。该框架包含了RabbitMQ
- 导入依赖
```xml
<!--AMQP依赖，包含RabbitMQ-->  
<dependency>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-amqp</artifactId>  
</dependency>
```
- 配置RabbitMQ
在**每一个**微服务中配置MQ，用于进行MQ消息传递
```yaml
spring:  
  rabbitmq:  
    host: 47.96.117.5  
    port: 5672  
    username: itheima  
    password: 123321  
    virtual-host: /
```
- 发送消息
```java
  
@SpringBootTest  
public class SpringAmqpTest {  
  
    @Autowired  
    private RabbitTemplate rabbitTemplate;  
  
    @Test  
    public void testSimpleQueue(){  
        String queueName = "simple.queue";  
  
        String message = "hello, spring AMQP";  
  
        rabbitTemplate.convertAndSend(queueName,message);  
    }
}
```
- 监听消息
```java
package com.itheima.consumer.listener;  
  
  
import lombok.extern.slf4j.Slf4j;  
import org.springframework.amqp.rabbit.annotation.RabbitListener;  
import org.springframework.stereotype.Component;  
  
@Component  
@Slf4j  
public class SpringRabbitListener {  
  
    @RabbitListener(queues = "simple.queue")  
    public void listenSimpleQueueMessage(String msg) throws InterruptedException{  
        log.info("spring 消费者收到消息：[{}]",msg);  
    }
}
```
- 消费者消息**推送限制**

默认的广播是采用轮询的方式发送到队列中不同的监听对象，但是这样会造成性能差的机器消息堵塞。

使能推送限制可以解决服务器性能差异导致的性能好的机器处理太快而等不到消息，性能差的机器处理太慢而堆积太多消息。

在消费者服务中加入配置

在消费端中设置 `prefetch=n` ，表示一次从队列中获取 n 条消息，如果没处理完，则不会再获取消息
```yaml
spring:
  rabbitmq:
    listener:  
      direct:  
        prefetch: 1
```
## Fanout交换机
交换机可以将发布的消息按照指定的方式推送到队列，消费者可以在队列中收到消息。这样就可以通过交换机通知全部监听队列的服务，实现发送一条消息，全部微服务都可以执行。

Fanout是广播发送
![](images/Pasted%20image%2020240720151357.png)
- 消费者
```java
@Component  
@Slf4j  
public class SpringRabbitListener {  
  
    @RabbitListener(queues = "hello.queue1")  
    public void listenFanoutQueueMessage1(String msg) {  
        log.info("spring 消费者1--------收到消息：[{}]", msg);  
    }  
    // 如果这里是hello.queue1，则表示一个集群
    @RabbitListener(queues = "hello.queue2")  
    public void listenFanoutQueueMessage2(String msg) {  
        log.info("spring 消费者2--------收到消息：[{}]", msg);  
    }
}
```
- 发布者
```java
@SpringBootTest  
public class SpringAmqpTest {  
  
    @Autowired  
    private RabbitTemplate rabbitTemplate;  
  
    @Test  
    public void testSimpleQueue(){  
        String exchangeName = "amq.fanout";  
  
        String message = "hello, how are you";  
  
        rabbitTemplate.convertAndSend(exchangeName,"",message);  
    }
}
```
## Direct交换机
`Direct`交换机将消息根据路由规则推送到指定的队列中，被称为定向路由。定向路由的每一个队列都与交换机设置一个`bindingKey`，发布者发送消息时会指定消息的`RoutingKey`。交换机将消息路由到`BindingKey`与消息`RoutingKey`一致的队列。
![](images/Pasted%20image%2020240720153119.png)
- 绑定队列
![](images/Pasted%20image%2020240720153405.png)
- 消费者
```java
@Component  
@Slf4j  
public class SpringRabbitListener {  
  
    @RabbitListener(queues = "direct.queue1")  
    public void listenDirectQueueMessage1(String msg) {  
        log.info("spring 消费者1--------收到消息：[{}]", msg);  
    }  
    @RabbitListener(queues = "direct.queue2")  
    public void listenDirectQueueMessage2(String msg) {  
        log.info("spring 消费者2--------收到消息：[{}]", msg);  
    }
}
```
- 发布者
```java
  
@SpringBootTest  
public class SpringAmqpTest {  
  
    @Autowired  
    private RabbitTemplate rabbitTemplate;  
  
    @Test  
    public void testSimpleQueue(){  
        String exchangeName = "amq.direct";  
  
        String message = "绿色：绿水青三！！！！！！！！！";  
  
        rabbitTemplate.convertAndSend(exchangeName,"green",message);  
    }
}
```
![](images/Pasted%20image%2020240720154246.png)
## Topic交换机
topic交换机也是基于RoutingKey做消息路由。但是routingKey通常是多个单词的组合，并以`.`分割。能够实现类似于分组的功能
- `#`代指0个或多个单此
- `*`代指一个单词
![](images/Pasted%20image%2020240720154436.png)
## 代码声明交换机
![](images/Pasted%20image%2020240720155652.png)
```java
public class FanoutConf {  
  
    @Bean  
    public FanoutExchange fanoutExchange(){  
        return new FanoutExchange("hmall.fanout");  
    }  
    @Bean  
    public Queue fanoutQueue1(){  
        return new Queue("hmall-fanout.queue");  
    }  
    @Bean  
    public Binding fanoutBinding(Queue queue,FanoutExchange fanoutExchange){  
        return BindingBuilder.bind(queue).to(fanoutExchange).with("bindingKey");  
    }
}
```
## 💪注解声明队列、交换机和绑定
如果绑定关系中的队列和交换机没有声明的话，注解会帮我们自动生成。
```java
@Component  
@Slf4j  
public class SpringRabbitListener {  
  
    @RabbitListener(bindings = @QueueBinding(  
            value = @Queue("direct.queue1"),  
            exchange = @Exchange("amq.direct"),  
            key = {"red","blue"}  
    ))    
    public void listenDirectQueueMessage1(String msg) {  
        log.info("spring 消费者1--------收到消息：[{}]", msg);  
    }  
    @RabbitListener(bindings = @QueueBinding(  
            value = @Queue("direct.queue2"),  
            exchange = @Exchange("amq.direct"),  
            key = {"red","green"}  
    ))    
    public void listenDirectQueueMessage2(String msg) {  
        log.info("spring 消费者2--------收到消息：[{}]", msg);  
    }
}
```
## 消息转换器
![](images/Pasted%20image%2020240720161552.png)

# 业务改造
从经验上讲，以支付为例，如果其他业务与支付关系不大，则可以采用MQ通信的方式，而不是采用feign调用的方式。

改造前
```java
public void tryPayOrderByBalance(PayOrderFormDTO payOrderFormDTO) {  
    // 1.查询支付单  
    PayOrder po = getById(payOrderFormDTO.getId());  
    // 2.判断状态  
    if(!PayStatus.WAIT_BUYER_PAY.equalsValue(po.getStatus())){  
        // 订单不是未支付，状态异常  
        throw new BizIllegalException("交易已支付或关闭！");  
    }  
    // 3.尝试扣减余额  
    userFeign.deductMoney(payOrderFormDTO.getPw(), po.getAmount());  
  
  
    // 4.修改支付单状态  
    boolean success = markPayOrderSuccess(payOrderFormDTO.getId(), LocalDateTime.now());  
    if (!success) {  
        throw new BizIllegalException("交易已支付或关闭！");  
    }  
    // 5.修改订单状态  
    //todo 改为MQ消息发送  
    orderFeign.markOrderPaySuccess(po.getBizOrderNo());  
}
```
改造后
- 消息收发端配置文件
```yaml
spring:
  rabbitmq:  
    host: ${hm.db.host} # 你的虚拟机IP  
    port: 5672 # 端口  
    virtual-host: /  # 虚拟主机  
    username: itheima # 用户名  
    password: 123321 # 密码
```
-  公共配置类
```java
package com.hmall.common.config;  
  
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;  
import org.springframework.amqp.support.converter.MessageConverter;  
import org.springframework.context.annotation.Bean;  
import org.springframework.context.annotation.Configuration;  
  
@Configuration  
public class MQConfig {  
  
    @Bean  
    public MessageConverter messageConverter(){  
        return new Jackson2JsonMessageConverter();  
    }
}
```
-  ⚠配置扫描
```text
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\  
  com.hmall.common.config.MyBatisConfig,\  
  com.hmall.common.config.MvcConfig,\  
  com.hmall.common.config.MQConfig,\  
  com.hmall.common.config.JsonConfig
```
- 消息发送端
```java
   public void tryPayOrderByBalance(PayOrderFormDTO payOrderFormDTO) {  
        // 1.查询支付单  
        PayOrder po = getById(payOrderFormDTO.getId());  
        // 2.判断状态  
        if(!PayStatus.WAIT_BUYER_PAY.equalsValue(po.getStatus())){  
            // 订单不是未支付，状态异常  
            throw new BizIllegalException("交易已支付或关闭！");  
        }  
        // 3.尝试扣减余额  
        userFeign.deductMoney(payOrderFormDTO.getPw(), po.getAmount());  
  
  
        // 4.修改支付单状态  
        boolean success = markPayOrderSuccess(payOrderFormDTO.getId(), LocalDateTime.now());  
        if (!success) {  
            throw new BizIllegalException("交易已支付或关闭！");  
        }  
        // 5.修改订单状态  
        //todo 改为MQ消息发送  
//        orderFeign.markOrderPaySuccess(po.getBizOrderNo());  
        rabbitTemplate.convertAndSend("pay.direct","pay.success",po.getBizOrderNo());  
    }
```
- 消息接收端
```java
@Component  
@RequiredArgsConstructor  
public class PayStatusListener {  
  
    private final IOrderService orderService;  
  
    @RabbitListener(bindings = @QueueBinding(  
            value = @Queue(name = "order.pay.success.queue",durable = "true"),  
            exchange = @Exchange(name = "pay.direct",type = "direct"),  
            key = "pay.success"  
    ))  
    public void listenPaySuccess(Long orderId){  
        orderService.markOrderPaySuccess(orderId);  
    }
}
```
![](images/Pasted%20image%2020240720164621.png)
# MQ高级
## 消息发送可靠性
### 💪发送者重连
有的时候由于网络波动，可能会出现**发送者**连接MQ失败的情况。通过配置我们可以开启连接失败后的重连机制。但是该机制是采用阻塞式编程，影响业务的性能。
```yaml
spring:  
  rabbitmq:  
    host: ${hm.db.host} # rabbitmq服务主机IP  
    port: 5672 # 端口  
    virtual-host: /  # 虚拟主机  
    username: itheima # 用户名  
    password: 123321 # 密码  
    connection-timeout: 1s  
    template:  
      retry:  
        enabled: true # 开启超时重试机制  
        initial-interval: 1000ms #失败后的初始等待时间  
        max-attempts: 3 # 最大重试次数  
        multiplier: 1 # 失败后下次的等待时长倍数
```
### 💪发送者确认
SpringAMQP提供了Publisher Confirm和Publisher Return两种确认机制。开启确机制认后，当发送者发送消息给MQ后，MQ会返回确认结果给发送者。返回的结果有以下几种情况:

- 消息投递到了MQ，但是路由失败。此时会通过Publisher Return返回路由异常原因，然后返回`ACK`，告知投递成功。
- 临时消息投递到了MQ，并且入队成功，返回`ACK`，告知投递成功
- 持久消息投递到了MQ，并且入队完成持久化，返回`ACK`，告知投递成功
- 其它情况都会返回`NACK`，告知投递失败
![](images/Pasted%20image%2020240721193410.png)
- 配置
```yaml
spring:  
  rabbitmq:  
    publisher-confirm-type: correlated  
	# correlated MQ异步回调方法返回回执消息  
	# simple 同步阻塞等待MQ回执消息  
	publisher-returns: true
```
- 为每一个`RabbitTemplate`配置一个`ReturnCallback`，因此需要在项目启动过程中配置

returnCallback是消息投递到了MQ，但是路由失败，此时会回调returnCallback，但是会返回`ACK`。
```java
  
@Slf4j  
@Configuration  
@RequiredArgsConstructor  
public class MQConfig {  
  
    private final RabbitTemplate rabbitTemplate;  
  
    @PostConstruct  
    public void init(){  
        rabbitTemplate.setReturnsCallback(  
                returnedMessage -> {  
                    log.error("监听到了消息 return callback");  
                    log.debug("exchange: {}",returnedMessage.getExchange());  
                    log.debug("routingKey: {}",returnedMessage.getRoutingKey());  
                    log.debug("message: {}",returnedMessage.getMessage());  
                    log.debug("replyText: {}",returnedMessage.getReplyText());  
                }
            );    
        }
    }
}
```
- ConfirmCallback
发送消息，指定消息ID、消息`ConfirmCallback`。通过实现 ConfirmCallback 接口，消息发送到 Broker 后触发回调，确认消息是否到达 Broker 服务器，也就是只确认是否正确到达 Exchange 中
```java
@Test
public void testSimpleQueue() throws InterruptedException {  
  
    CorrelationData cd = new CorrelationData(UUID.randomUUID().toString());  
    cd.getFuture().addCallback(new ListenableFutureCallback<CorrelationData.Confirm>() {  
        @Override  
        public void onFailure(Throwable ex) {  
            log.error("spring amqp 处理确认结果异常", ex);  
        }  
        @Override  
        public void onSuccess(CorrelationData.Confirm result) {  
            if (result.isAck()) {  
                log.debug("收到ConfirmCallback，消息发送成功！");  
            } else {  
                log.error("收到ConfirmCallback，消息发送失败！原因：{}", result.getReason());  
            }        
        }    
    });  
    String exchangeName = "amq.direct";  
  
    String message = "绿色：绿水青三！！！！！！！！！";  
  
    rabbitTemplate.convertAndSend(exchangeName, "red", message, cd);  
  
    Thread.sleep(5000);  
}
```
## MQ可靠性
### 宕机问题
队列持久化+交换机持久化+消息持久化=MQ高可靠

spring Amqp发送消息默认是持久化消息，rabbit中创建队列与交换机默认是持久化的。
### 💪LazyQueue
LazyQueue收到消息后直接写入磁盘，不再存储到内存

***消费者***获取消息时会从磁盘读取

但由于读写会降低运行速度，LazyQueue会监听消息读写速度，速度较快则自动缓存一部分到内存。

`LazyQueue`注入 (消费者)
```java
@Bean  
public Queue lazyQueue() {  
    return QueueBuilder  
            .durable("lazy.queue").  
            lazy().  
            build();  
}
```
`LazyQueue`注解
```java
@RabbitListener(bindings = @QueueBinding(  
        value = @Queue(  
                name="direct.queue1",  
                durable = "true",  
                arguments = @Argument(name="x-queue-mode",value = "lazy")  
        ),        
        exchange = @Exchange("amq.direct"),  
        key = {"red","blue"}  
))  
public void listenDirectQueueMessage1(String msg) {  
    log.info("spring 消费者1--------收到消息：[{}]", msg);  
}
```
## 消费者接收可靠性
### 消费者确认机制
![](images/Pasted%20image%2020240722205411.png)
`SpringAMQP`已经实现了消息确认功能。并允许我们通过配置文件选择ACK处理方式，有三种方式:

- `none`:不处理。即消息投递给消费者后立刻ack，消息会立刻从MQ删除。非常不安全，不建议使用

- `manual`:手动模式。需要自己在业务代码中调用api，发送ack或reject，存在业务入侵，但更灵活

- `auto`:自动模式。SpringAMQP利用AOP对我们的消息处理逻辑做了环绕增强，
当业务正常执行时则自动返回ack当业务出现异常时，根据异常判断返回不同结果:
- 如果是业务异常，会自动返回`nack`，如果返回`nack`，则会一直重试
- 如果是消息处理或校验异常，自动返回`reject`
```yaml
spring:  
  rabbitmq:  
    host: 47.96.117.5  
    port: 5672  
    username: itheima  
    password: 123321  
    virtual-host: /  
    listener:  
      direct:  
        prefetch: 1  
	# 消费确认
      simple:  
        prefetch: 1  
        acknowledge-mode: none
```
### 💪消费者失败重试
```yaml
spring:  
  rabbitmq:  
    host: 47.96.117.5  
    port: 5672  
    username: itheima  
    password: 123321  
    virtual-host: /  
    listener:  
      direct:  
        prefetch: 1  
      simple:  
        prefetch: 1  
        acknowledge-mode: none  
        retry:  
          enabled: true # 是否开启重试机制  
          initial-interval: 1000ms # 第一次重试间隔时间  
          max-attempts: 3 # 最大重试次数  
          multiplier: 1 # 每次重试间隔时间的倍数  
          stateless: true # true无状态，flase有状态。如果业务中包含事务，则改为false
```
![](images/Pasted%20image%2020240909001236.png)
```java
  
@Configuration  
public class ErrorMQConf {  
  
    @Bean  
    public DirectExchange errorExchange(){  
        return new DirectExchange("error.direct");  
    }  
    @Bean  
    public Queue errorQueue(){  
        return new Queue("error.queue");  
    }  
    @Bean  
    public Binding errorQueueBinding(Queue errorQueue,DirectExchange errorExchange){  
        return BindingBuilder.bind(errorQueue).to(errorExchange).with("error");  
    }  
    @Bean  
    public MessageRecoverer messageConverter(RabbitTemplate rabbitTemplate){  
        return new RepublishMessageRecoverer(rabbitTemplate,"error.direct","error");  
    }
}
```
### 业务幂等性
幂等是一个数学概念，用函数表达式描述为$f(x)=f(f(x))$。在程序开发中，则是指同一个业务，执行一次或者多次对业务状态的影响是一致的。

![](images/Pasted%20image%2020240909222941.png)
如果由于网络波动导致MQ发送多次消息，而多次消息会导致业务执行多次。在非幂等业务中，由于多次执行，那么库存可能发生扣减。

> 唯一消息id

给每条消息都创建一个唯一id，利用id区分是否为重复消息

- 每条消息都生成一个唯一id，与消息一起投递给消费者
- 消费者接收到消息后处理自己的业务，业务处理完成后将消息id保存到数据库中
- 在下次收到消息时则**去数据库查询**，判断是否存在，存在则为重复消息放弃处理
发送者发送时生成唯一id
```java
@SpringBootApplication  
public class PublisherApplication {  
    public static void main(String[] args) {  
        SpringApplication.run(PublisherApplication.class);  
            }  
    @Bean  
    public MessageConverter messageConverter(){  
        Jackson2JsonMessageConverter jjmc = new Jackson2JsonMessageConverter();  
        jjmc.setCreateMessageIds(true);  
        return jjmc;  
    }
}
```
消费者接收时获取id
```java
public void listenDirectQueueMessage1(Message msg) {  
    log.info("spring 消费者1--------收到ID为{}的消息：[{}]",   
			msg.getMessageProperties().getMessageId(),  
			msg.getBody()
	);  
}
```
> 业务判断

以余额支付业务为例
![](images/Pasted%20image%2020240909224234.png)

![](images/Pasted%20image%2020240909224508.png)
## 延迟消息
**延迟消息**：发送者发送消息时指定一个时间，消费者不会理科收到消息，而是在指定时间后才收到消息

**延迟任务**：设置在一定时间后才执行的任务
### 死信交换机
- 消费者使用basic.reject或者basic.nack声明消费失败，并且消息的requeue参数设置为false
- 消息是一个过期消息，超时无人消费
- 要投递的队列堆积满了，最早的消息可能成为死信
如果队列通过dead-letter-exchange属性指定了一个交换机，那么该队列中的死信就会投递到这个属性指定的交换机中

**模拟延迟**：不给普通队列绑定消费者，超时无人消费则会发送到死信交换机中，有死信交换机的消费者消费，即完成了延迟业务

**未绑定消费者的正常交换机**
```java
  
@Configuration  
public class NormalConf {  
  
    @Bean  
    public DirectExchange normalExchange(){  
        return new DirectExchange("normal.direct");  
    }  
    @Bean  
    public Queue normalQueue(){  
        return QueueBuilder.durable("normal.queue").deadLetterExchange("dlx.direct").build();  
    }  
    @Bean  
    public Binding normalQueueBinding(Queue normalQueue,DirectExchange normalExchange){  
        return BindingBuilder.bind(normalQueue).to(normalExchange).with("hi");  
    }  
}
```
**死信交换机**
```java
@RabbitListener(bindings = @QueueBinding(  
        value = @Queue("dlx.queue"),  
        exchange = @Exchange("dlx.direct"),  
        key = {"hi"}  
))  
public void listenDlxQueue(String msg) {  
    log.info("死信交换机的消费者--------收到消息：[{}]", msg);  
}
```
**发送延迟消息**
```java
@Test  
void testSendDelayMessage(){  
    rabbitTemplate.convertAndSend("normal.direct", "hi", "hello", message -> {  
        message.getMessageProperties().setExpiration("10000");  //10s == 10000ms
        return message;  
    });
}
```
### 💪延迟消息插件
该插件可以将普通交换机改造为支持延迟消息功能的交换机，当消息投递到交换机后可以暂存一段时间，到期后再投递到队列
- 安装插件
- 启动插件
- 定义延迟交换机
```java
@RabbitListener(bindings = @QueueBinding(  
        value = @Queue("delay.queue"),  
        exchange = @Exchange(value = "delay.direct",delayed = "true"),  
        key = {"delay"}  
))  
public void listenDelayQueue(String msg) {  
    log.info("delay交换机的消费者--------收到消息：[{}]", msg);  
}
```
- 发送延迟消息
```java
@Test  
void testSendDelayMessage(){  
    rabbitTemplate.convertAndSend("delay.direct", "delay", "hello", message -> {  
        message.getMessageProperties().setDelay("10000");  //10s == 10000ms
        return message;  
    });
}
```
