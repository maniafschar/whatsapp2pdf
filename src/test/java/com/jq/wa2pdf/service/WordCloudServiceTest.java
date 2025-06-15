package com.jq.wa2pdf.service;

import java.awt.FontFormatException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.jq.wa2pdf.TestConfig;
import com.jq.wa2pdf.WhatsApp2PdfApplication;
import com.jq.wa2pdf.service.WordCloudService.Token;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
		WhatsApp2PdfApplication.class,
		TestConfig.class }, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
				"server.port=9001", "server.servlet.context-path=/rest" })
public class WordCloudServiceTest {
	@Autowired
	private WordCloudService wordCloudService;

	@Test
	public void createImage() throws IOException, FontFormatException {
		// given
		final List<Token> tokens = Arrays.asList(new Token("haus"), new Token("garten"), new Token("hund"),
				new Token("katze"), new Token("maus"), new Token("tür"), new Token("büro"), new Token("stuhl"),
				new Token("reis"), new Token("bett"), new Token("schrank"), new Token("liege"), new Token("teppich"),
				new Token("bild"), new Token("gehen"), new Token("aufstehen"));
		for (int i = 0; i < tokens.size(); i++)
			tokens.get(i).count = i;

		// when
		wordCloudService.createImage(tokens, 0, tokens.size(), Paths.get("test.png").toAbsolutePath());

		// then: watch image
	}
}