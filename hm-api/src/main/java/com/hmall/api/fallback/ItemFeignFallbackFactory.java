package com.hmall.api.fallback;

import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.api.feign.ItemFeign;
import com.hmall.common.utils.CollUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;
import java.util.List;

@Slf4j
public class ItemFeignFallbackFactory implements FallbackFactory<ItemFeign> {
    @Override
    public ItemFeign create(Throwable cause) {
        return new ItemFeign() {
            @Override
            public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
                log.error("查询商品失败",cause);
                return CollUtils.emptyList();
            }

            @Override
            public void deductStock(List<OrderDetailDTO> detailDTOS) {
                log.error("111");
                throw new RuntimeException(cause);
            }
        };
    }
}
