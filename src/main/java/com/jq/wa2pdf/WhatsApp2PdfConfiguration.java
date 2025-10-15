package com.jq.wa2pdf;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.jq.wa2pdf.entity.Ticket;
import com.jq.wa2pdf.service.AdminService;
import com.jq.wa2pdf.util.Utilities;

@Configuration
public class WhatsApp2PdfConfiguration implements AsyncConfigurer {
	private static final String[] allowedOrigins = {
			"http://localhost:9000",
			"https://wa2pdf.com"
	};

	@Autowired
	private AdminService adminService;

	@Override
	public Executor getAsyncExecutor() {
		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.initialize();
		executor.setWaitForTasksToCompleteOnShutdown(true);
		return executor;
	}

	@Override
	@Nullable
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return new AsyncUncaughtExceptionHandler() {
			@SuppressWarnings("null")
			@Override
			public void handleUncaughtException(final Throwable ex, final Method method, final Object... obj) {
				WhatsApp2PdfConfiguration.this.adminService.createTicket(
						new Ticket(Ticket.ERROR + AsyncUncaughtExceptionHandler.class.getSimpleName() + "\n" +
								method.toGenericString() + "\n" + (obj == null ? ""
										: Arrays.asList(obj).stream().map(e -> e == null ? "[null]" : e.toString())
												.collect(Collectors.joining(", ")) + "\n")
								+ Utilities.stackTraceToString(ex)));
			}
		};
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@SuppressWarnings("null")
			@Override
			public void addCorsMappings(final CorsRegistry registry) {
				registry.addMapping("/**").allowedOriginPatterns(allowedOrigins)
						.allowedHeaders("content-type", "x-requested-with", "user")
						.exposedHeaders("content-disposition")
						.allowedMethods("GET", "PUT", "POST", "OPTIONS", "DELETE");
			}
		};
	}
}
