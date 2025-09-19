package com.jq.wa2pdf.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.jq.wa2pdf.TestConfig;
import com.jq.wa2pdf.WhatsApp2PdfApplication;
import com.jq.wa2pdf.service.AiService.AiType;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
		WhatsApp2PdfApplication.class,
		TestConfig.class }, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
				"server.port=9001", "server.servlet.context-path=/rest" })
@ActiveProfiles("test")
public class AiServiceTest {
	@Autowired
	private AiService aiService;

	@Test
	public void summerize() throws IOException {
		// given
		AiService.type = AiType.None;
		final String text = IOUtils.toString(this.getClass().getResourceAsStream("/_chat.txt"), StandardCharsets.UTF_8);

		// when
		final String summary = this.aiService.summerize(text);

		// then
		assertNotNull(summary);
		assertTrue(summary.length() < 100);
	}
}