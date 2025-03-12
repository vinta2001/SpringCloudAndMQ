## 🤚我的博客

- 欢迎光临我的博客：<a>`https://blog.csdn.net/qq_52434217?type=blog`</a>

## 🥛前言
微服务框架中`CircuitBreaker`的简单用法。
# 📖RateLimiter

## 🔷常见限流算法

- 漏桶限流
    

漏桶限流的原理是将访问量比作水流量，同时漏斗出水口流量恒定。当访问量过大到达漏斗的最大值后，超过的访问量会被限制而不允许访问。

![](https://niipetoekho.feishu.cn/space/api/box/stream/download/asynccode/?code=Nzk0MGZlYTI1NWU2NWZlM2ViYjYxN2VkNTllYTExODVfNjlTR0M1a0NEbEgzVWhEY1p5NVdQR0tVSUhvdDMwQk1fVG9rZW46RUw0eWJUMTBFb0JXZVd4SWZxMGNQMldOblJlXzE3MTIwMzAyMzI6MTcxMjAzMzgzMl9WNA)

- 令牌桶
    

令牌桶的思想是客户端每访问一次服务器就少一个令牌，当服务器令牌减少到0后，客户端的请求没有令牌可以携带，便无法请求服务器。

![](https://niipetoekho.feishu.cn/space/api/box/stream/download/asynccode/?code=ZTZlNjY1Nzc3MmRiMDViNGQ4ZDI4YjljNzA5ZDc1YWJfOE5CbW10RmZ3aWJEaDhBZG96Z3gxNGhWS1o3UXEwZnFfVG9rZW46SnRHWGIyOTJNb0tqZGh4YkpmUGNndUlabkFlXzE3MTIwMzAyMzI6MTcxMjAzMzgzMl9WNA)

## 🔷参数配置

- 依赖导入
    

```XML
<dependency>    
    <groupId>org.springframework.cloud</groupId>    
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>    
    <version>3.1.0</version>  
</dependency>  
  
<dependency>    
    <groupId>io.github.resilience4j</groupId>    
    <artifactId>resilience4j-bulkhead</artifactId>    
    <version>2.2.0</version>  
</dependency>  
  
<dependency>    
    <groupId>io.github.resilience4j</groupId>    
    <artifactId>resilience4j-ratelimiter</artifactId>    
    <version>2.2.0</version>  
</dependency>
```

- 参数配置
```YAML
resilience4j:
  ratelimiter:
    configs:
      default:
        # 刷新周期内允许的请求数
        limit-for-period: 2
        # 限流器每隔limit-refresh-period刷新一次，
        # 每次刷新将最大允许请求次数重置为limit-for-period
        limit-refresh-period: 1s
        # 线程等待权限的默认等待时间
        timeout-duration: 1
    instances:
      pay-service:
        base-config: default
```

- 客户端代码
    

```Java
@GetMapping("/feign/pay/bulkhead/{id}")
@RateLimiter(name = "pay-service", fallbackMethod = "myRateLimiter")
public String myRateLimiter(@PathVariable("id")Integer id){
    return  payFeignAPI.myCircuitBreaker(id)+" RateLimiter";
}

public String myRateLimiter(Integer id, Throwable throwable){
    return "系统繁忙，请稍后再试~ from THREAD-POOL";
}
```

- 限流测试
    

此时连续短时间内发送请求时会出现限流效果

下面为正常访问

![](https://niipetoekho.feishu.cn/space/api/box/stream/download/asynccode/?code=MDcwNTU5MzczOWZiNDY1YjlkMTA2NjQzYzI5MWZjODZfOEQ3VXluZmJWVDRCWW4wY3hMWHRwWmdwVEZWa0tVTkNfVG9rZW46QnVsSGJiNnpqbzY0WXF4NXNIS2MzMFU1bjNnXzE3MTIwMzAyMzI6MTcxMjAzMzgzMl9WNA)

下图为限流结果

![](https://niipetoekho.feishu.cn/space/api/box/stream/download/asynccode/?code=MmI4N2VhM2VmNDlmYTQ1NjBkYmE2NWIyNzVhNTQxYTJfZzh4YjF4Z3hKM0M5ODB2c3hsbkhNN3lINk40NW0zOU9fVG9rZW46QnhPN2JRU2lHb2pPaWF4UzkzTGMwZVNkblhjXzE3MTIwMzAyMzI6MTcxMjAzMzgzMl9WNA)

## 🔷END

至此，本章主要介绍了rateLimiter的基本使用，有问题可以留言。

### 公众号

欢迎关注小夜的公众号，一个立志什么都能会的研究生。
 

![微信公众号](https://img-blog.csdnimg.cn/img_convert/5a5d07d81cdc0f91842cb784cc75bb39.png#pic_center =300x300)

---