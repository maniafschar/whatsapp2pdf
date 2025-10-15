package com.jq.wa2pdf;

import java.io.File;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.jq.wa2pdf.entity.Feedback;
import com.jq.wa2pdf.repository.Repository;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
		WhatsApp2PdfApplication.class,
		TestConfig.class }, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
				"server.port=9001", "server.servlet.context-path=/rest" })
@ActiveProfiles("test")
public class Application {
	private static String url = "http://localhost:9000/";
	private WebDriver driver;

	@Autowired
	private Repository repository;

	@Test
	public void run() throws Exception {
		Thread.sleep(600000);
	}

	@BeforeEach
	public void beforeEach() throws Exception {
		new ProcessBuilder("./web.sh", "start").start();
		this.driver = createWebDriver(400, 900);
		this.driver.get(url);
		final Feedback feedback = new Feedback();
		feedback.setNote("abc");
		feedback.setRating((short) 88);
		feedback.setName("mani");
		feedback.setEmail("mani.afschar@jq-consulting.de");
		feedback.setPin("123456");
		feedback.setModifiedAt(new Timestamp(System.currentTimeMillis()));
		feedback.setVerified(true);
		this.repository.save(feedback);
	}

	@AfterEach
	public void afterEach() throws Exception {
		this.driver.close();
		new ProcessBuilder("./web.sh", "stop").start();
		final String dir = System.getProperty("java.io.tmpdir") + File.separatorChar;
		for (final String file : Paths.get(dir).toFile().list()) {
			if (file.startsWith("whatsapp2pdf_") && new File(dir + file).isDirectory())
				FileUtils.forceDelete(new File(dir + file));
		}
	}

	static WebDriver createWebDriver(final int width, final int height) {
		final ChromeOptions options = new ChromeOptions();
		final Map<String, Object> deviceMetrics = new HashMap<>();
		deviceMetrics.put("pixelRatio", 1.0);
		deviceMetrics.put("width", width);
		deviceMetrics.put("height", height);
		options.addArguments("user-data-dir=./chrome");
		return new ChromeDriver(options);
	}
}
