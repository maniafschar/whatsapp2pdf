package com.jq.wa2pdf;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
		WhatsApp2PdfApplication.class,
		TestConfig.class }, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
				"server.port=9001", "server.servlet.context-path=/rest" })
public class WhatsApp2PdfApplicationTest {
	private static String url = "http://localhost:9000/";

	@Test
	public void run() throws Exception {
		Thread.sleep(600000);
	}

	@BeforeEach
	public void beforeEach() throws Exception {
		new ProcessBuilder("./web.sh", "start").start();
		createWebDriver(600, 900).get(url);
	}

	@AfterEach
	public void afterEach() throws Exception {
		new ProcessBuilder("./web.sh", "stop").start();
	}

	static WebDriver createWebDriver(int width, int height) {
		final ChromeOptions options = new ChromeOptions();
		final Map<String, Object> deviceMetrics = new HashMap<>();
		deviceMetrics.put("pixelRatio", 1.0);
		deviceMetrics.put("width", width);
		deviceMetrics.put("height", height);
		options.addArguments("user-data-dir=./chrome");
		return new ChromeDriver(options);
	}
}