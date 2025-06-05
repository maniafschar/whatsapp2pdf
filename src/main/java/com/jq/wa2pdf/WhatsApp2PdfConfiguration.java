package com.jq.wa2pdf;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WhatsApp2PdfConfiguration implements AsyncConfigurer {
	private static final String[] allowedOrigins = {
			"http://localhost:9000"
	};

	@Override
	public Executor getAsyncExecutor() {
		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.initialize();
		return executor;
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		System.out.println("Configuring CORS with allowed origins: " + String.join(", ", allowedOrigins));
		return new WebMvcConfigurer() {
			@SuppressWarnings("null")
			@Override
			public void addCorsMappings(final CorsRegistry registry) {
				registry.addMapping("/**").allowedOriginPatterns(allowedOrigins)
						.allowedHeaders("clientid", "content-type", "password", "salt", "secret", "user", "webcall")
						.allowedMethods("GET", "PUT", "POST", "PATCH", "OPTIONS", "DELETE");
			}
		};
	}
}