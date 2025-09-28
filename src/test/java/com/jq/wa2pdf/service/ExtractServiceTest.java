package com.jq.wa2pdf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.jq.wa2pdf.TestConfig;
import com.jq.wa2pdf.WhatsApp2PdfApplication;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
		WhatsApp2PdfApplication.class,
		TestConfig.class }, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
				"server.port=9001", "server.servlet.context-path=/rest" })
@ActiveProfiles("test")
public class ExtractServiceTest {
	@Autowired
	private ExtractService extractService;

	@Test
	public void regex() {
		// given
		final Pattern p = Pattern.compile(this.extractService.getPatternStart("[0-9\\/\\-\\.]*"));
		final String text = "8/21/25, 11:18 PM - abc: def";

		// when
		final Matcher matcher = p.matcher(text);

		// then
		assertTrue(matcher.matches());
		assertEquals("8/21/25, 11:18 PM -", matcher.group(1));
	}
}
