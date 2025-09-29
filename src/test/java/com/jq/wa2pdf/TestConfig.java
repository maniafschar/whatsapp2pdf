package com.jq.wa2pdf;

import javax.sql.DataSource;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.jq.wa2pdf.service.AiService;
import com.jq.wa2pdf.service.AiService.AiType;

@Profile("test")
@TestConfiguration
@EnableTransactionManagement
public class TestConfig {
	public TestConfig() {
		AiService.type = AiType.None;
	}

	@Bean
	public DataSource getDataSource() throws Exception {
		final DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setUrl("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1");
		return dataSource;
	}
}
