# Loadbalancer
## Loadbalancer与nginx做负载均衡的区别
Loadbalancer是本地负载均衡，在调用微服务接口时候，会在注册中心上获取注册信息服务列表之后缓存到JVM本地，从而在本地实现RPC远程服务调用技术。

nginx是服务端负载均衡。客户端所有请求都会交给nginx，然后由nginx实现转发请求，即负载均衡是由服务端实现的。


## 负载均衡的实现
### 导入依赖
在客户端导入依赖
```xml
<dependency>  
    <groupId>org.springframework.cloud</groupId>  
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>  
    <version>4.0.0</version>  
</dependency>
```
导入之后就可以实现轮询式负载均衡。但是实际发现不需要加入该注解也可以实现轮询均衡。

### 切换均衡算法
- 配置均衡算法配置类
```java
public class CustomLoadBalancerConfiguration { 
	@Bean 
	ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(Environment environment, 
										LoadBalancerClientFactory loadBalancerClientFactory) { 
		String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME); 
		return new RandomLoadBalancer(loadBalancerClientFactory
					.getLazyProvider(name, ServiceInstanceListSupplier.class), name); 
	} 
}
```
- 配置均衡算法参数
在`@LoadBalancerClient`注解中配置需要均衡的服务，并且传入需要配置的算法
```java
@Configuration  
@LoadBalancerClient(value = "pay-service", configuration = CustomLoadBalancerConfiguration.class)  
public class RestConfig {  
    @Bean  
    @LoadBalanced    
    public RestTemplate restTemplate() {  
        return new RestTemplate();  
    }
}
```
这里也可以直接将两个配置放到一起，但是官方不建议。
```java
@Configuration  
@LoadBalancerClient(value = "pay-service", configuration = RestConfig.class)  
public class RestConfig {  
    @Bean  
    @LoadBalanced    
    public RestTemplate restTemplate() {  
        return new RestTemplate();  
    }  
    @Bean  
    ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(Environment environment,  
                                        LoadBalancerClientFactory loadBalancerClientFactory) {  
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);  
        return new RandomLoadBalancer(loadBalancerClientFactory  
                .getLazyProvider(name, ServiceInstanceListSupplier.class),  
                name);  
    }
}
```
