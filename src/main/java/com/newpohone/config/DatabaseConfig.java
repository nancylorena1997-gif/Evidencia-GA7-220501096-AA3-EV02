package com.newpohone.config;

import com.newpohone.database.DatabaseBootstrap;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DatabaseConfig {

    @Bean(destroyMethod = "close")
    public DataSource dataSource() {
        String url = DatabaseBootstrap.initialize();
        HikariConfig config = new HikariConfig();
        config.setPoolName("newphone-sqlite");
        config.setJdbcUrl(url);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.setConnectionInitSql("PRAGMA foreign_keys = ON");
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
