package com.hmall.gateway.routers;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouterLoader {

    private final NacosConfigManager nacosConfigManager;

    private final RouteDefinitionWriter writer;
    private final Set<String> routerIds = new HashSet<>();
    private final String dataId = "gateway-router.json";
    private final String group = "DEFAULT_GROUP";
    private final Long timeout = 5000L;

    @PostConstruct
    public void initRouterConfigListener() throws NacosException {
        // 项目启动时，先拉取一次配置，并添加配置监听器
        String configInfo = nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dataId, group, timeout, new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    /**
                     * 监听到配置变更，需要更新路由表
                     * @param configInfo;
                     */
                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        updateConfigInfo(configInfo);
                    }
                });

        //第一次读取到的配置，也需要更新路由表
        updateConfigInfo(configInfo);
    }

    public void updateConfigInfo(String configInfo){
        // 解析配置文件，转换为json
        log.debug("监听到路由信息变更：{}",configInfo);
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
        // 删除旧的路由表
        for(String routerId : routerIds){
            writer.delete(Mono.just(routerId));
        }
        // 更新路由信息
        for (RouteDefinition routeDefinition : routeDefinitions){
            writer.save(Mono.just(routeDefinition)).subscribe();
            //记录路由id，便于下次删除
            routerIds.add(routeDefinition.getId());
        }
    }
}
