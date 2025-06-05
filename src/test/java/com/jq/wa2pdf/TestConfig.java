package com.jq.wa2pdf;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.sql.DataSource;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Profile("test")
@TestConfiguration
@EnableTransactionManagement
public class TestConfig {
	@Bean
	public DataSource getDataSource() throws Exception {
		final DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setUrl("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1");
		dataSource.getConnection()
				.prepareStatement(
						"CREATE ALIAS IF NOT EXISTS WEEKDAY FOR \"" + getClass().getName() + ".weekday\";")
				.executeUpdate();
		dataSource.getConnection()
				.prepareStatement(
						"CREATE ALIAS IF NOT EXISTS TO_DAYS FOR \"" + getClass().getName() + ".toDays\";")
				.executeUpdate();
		dataSource.getConnection()
				.prepareStatement(
						"CREATE ALIAS IF NOT EXISTS SUBSTRING_INDEX FOR \"" + getClass().getName()
								+ ".substringIndex\";")
				.executeUpdate();
		return dataSource;
	}

	public static int toDays(final Connection connection, final Timestamp timestamp)
			throws SQLException {
		return 0;
	}

	public static int weekday(final Connection connection, final Timestamp timestamp)
			throws SQLException {
		return 1;
	}

	public static String substringIndex(final Connection connection, final String s, final String s2, final int i) {
		return s;
	}
}