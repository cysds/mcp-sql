package com.cysds.domain.router;

import com.cysds.domain.entity.ConnectionEntity;
import com.cysds.domain.service.connection.DynamicDataSourceService;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * &#064;author:  谢玮杰
 * &#064;description:  动态路由，根据传入的数据类型自动路由到对应的数据源类型
 * &#064;create:  2025-08-03 10:22
 **/
@Component
public class DynamicDataSourceRouter {

    private final Map<ConnectionEntity.DbType, DynamicDataSourceService<?>> serviceMap;

    @Autowired
    public DynamicDataSourceRouter(List<DynamicDataSourceService<?>> services) {
        serviceMap = services.stream()
                .collect(Collectors.toMap(DynamicDataSourceService::getDbType, s -> s));
    }

    /**
     * 根据传入的 ConnectionEntity，自动路由到对应的 DataSourceService
     */
    @SuppressWarnings("unchecked")
    public <E extends ConnectionEntity> HikariDataSource createDataSource(E entity) {
        DynamicDataSourceService<E> svc =
                (DynamicDataSourceService<E>) serviceMap.get(entity.getType());
        if (svc == null) {
            throw new IllegalArgumentException("不支持的数据库类型: " + entity.getType());
        }
        return svc.createDataSource(entity);
    }

    public <E extends ConnectionEntity> Connection getConnection(E entity) throws SQLException {
        return createDataSource(entity).getConnection();
    }
}
