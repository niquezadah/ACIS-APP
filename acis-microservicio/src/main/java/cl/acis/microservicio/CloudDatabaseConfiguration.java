package cl.acis.microservicio;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "acis.cloud.database", havingValue = "true")
class CloudDatabaseConfiguration {
    @Bean
    HikariDataSource dataSource(Environment environment) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(environment.getRequiredProperty("spring.datasource.url"));
        config.setUsername(environment.getRequiredProperty("spring.datasource.username"));
        config.setPassword(environment.getRequiredProperty("spring.datasource.password"));
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(0);
        config.setInitializationFailTimeout(-1);
        config.setConnectionTimeout(55_000);
        config.addDataSourceProperty("connectTimeout", "5");
        return new HikariDataSource(config);
    }
}
