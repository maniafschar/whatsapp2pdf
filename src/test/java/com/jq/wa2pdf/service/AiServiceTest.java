package com.jq.wa2pdf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.jq.wa2pdf.TestConfig;
import com.jq.wa2pdf.WhatsApp2PdfApplication;
import com.jq.wa2pdf.service.AiService.AiSummary;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
		WhatsApp2PdfApplication.class,
		TestConfig.class }, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
				"server.port=9001", "server.servlet.context-path=/rest" })
@ActiveProfiles("test")
public class AiServiceTest {
	@Autowired
	private AiService aiService;
	private final Set<String> users = new HashSet<>();
	private final String romeo = "Romeo";
	private final String julia = "Julia";

	@BeforeEach
	public void setUp() {
		this.users.add(this.julia);
		this.users.add(this.romeo);
	}

	@Test
	public void summerize() throws IOException {
		// given
		final String text = IOUtils.toString(this.getClass().getResourceAsStream("/_chat.txt"), StandardCharsets.UTF_8);

		// when
		final AiSummary summary = this.aiService.summerize(text, this.users);

		// then
		assertNull(summary);
	}

	@Test
	public void parseAdjectives_1() {
		// given
		final String text = "yxz\n########\n**Romeo:**\n" +
				"* **Adjectives:** Assertive, guarded, playful\n" +
				"* **Emojis:** 😉, 😁, 👌\n" +
				"**Julia:**\n" +
				"* **Adjectives:** Persistent, longing, provocative\n" +
				"* **Emojis:** 🎉, 🏋️, 🥵";

		// when
		final AiSummary summary = this.aiService.parseAdjectives(text, this.users);

		// then
		assertEquals("[assertive, guarded, playful]", summary.adjectives.get(this.romeo).toString());
		assertEquals("[😉, 😁, 👌]", summary.emojis.get(this.romeo).toString());
		assertEquals("[persistent, longing, provocative]", summary.adjectives.get(this.julia).toString());
		assertEquals("[🎉, 🏋️, 🥵]", summary.emojis.get(this.julia).toString());
		assertEquals("yxz\n########", summary.text);
	}

	@Test
	public void parseAdjectives_2() {
		// when
		final String text = "abc\n\ndef.\n########\n\n"
				+ "**Julia:**\n" +
				"*   Gefühlvoll 🎉\n" +
				"*   Loyal 🏋️\n" +
				"*   Stark 🥵\n" +
				"\n" +
				"**ROmeo:**\n" +
				"*   Leidenschaftlich 😉\n" +
				"*   Emotional 😁\n" +
				"*   Unsicher 👌";

		// when
		final AiSummary summary = this.aiService.parseAdjectives(text, this.users);

		// then
		assertEquals("[leidenschaftlich, emotional, unsicher]", summary.adjectives.get(this.romeo).toString());
		assertEquals("[😉, 😁, 👌]", summary.emojis.get(this.romeo).toString());
		assertEquals("[gefühlvoll, loyal, stark]", summary.adjectives.get(this.julia).toString());
		assertEquals("[🎉, 🏋\ufe0f, 🥵]", summary.emojis.get(this.julia).toString());
		assertEquals("abc\n\ndef.\n########", summary.text);
	}

	@Test
	public void parseAdjectives_3() {
		// when
		final String text = "abc\n\ndef.\n########\n"
				+ "**romeo:**\n"
				+ "* **adjektive:** leidenschaftlich, sehnsüchtig, kämpferisch\n"
				+ "* **emojis:** ❤️, 😘, 🔥\n"
				+ "**julia:**\n"
				+ "* **adjektive:** zärtlich, emotional, unsicher\n"
				+ "* **emojis:** 😘, 🥺, 💖";
		final String name = this.julia + " Klöckner";
		this.users.remove(this.julia);
		this.users.add(name);

		// when
		final AiSummary summary = this.aiService.parseAdjectives(text, this.users);

		// then
		assertEquals("[leidenschaftlich, sehnsüchtig, kämpferisch]", summary.adjectives.get(this.romeo).toString());
		assertEquals("[❤️, 😘, 🔥]", summary.emojis.get(this.romeo).toString());
		assertEquals("[zärtlich, emotional, unsicher]", summary.adjectives.get(name).toString());
		assertEquals("[😘, 🥺, 💖]", summary.emojis.get(name).toString());
		assertEquals("abc\n\ndef.\n########", summary.text);
	}

	@Test
	public void parseAdjectives_4() {
		// when
		final String text = "abc\n\ndef.\n########\n"
				+ "Julia: **emotional, leidenschaftlich, nachdenklich** ❤️😘🔥\n"
				+ "Romeo: **sehnsüchtig, aufgewühlt, aufrichtig** 😘🥺💖";

		// when
		final AiSummary summary = this.aiService.parseAdjectives(text, this.users);

		// then
		assertEquals("[sehnsüchtig, aufgewühlt, aufrichtig]", summary.adjectives.get(this.romeo).toString());
		assertEquals("[😘, 🥺, 💖]", summary.emojis.get(this.romeo).toString());
		assertEquals("[emotional, leidenschaftlich, nachdenklich]", summary.adjectives.get(this.julia).toString());
		assertEquals("[❤️, 😘, 🔥]", summary.emojis.get(this.julia).toString());
		assertEquals("abc\n\ndef.\n########", summary.text);
	}

	@Test
	public void parseAdjectives_5() {
		// when
		final String text = "abc\n\ndef.\n########\n"
				+ "Romeo: Liebevoll, leidend, hoffnungsvoll 🥰😔❤️\n"
				+ "Julia: Emotional, ambivalent, stark 😥😌💞";

		// when
		final AiSummary summary = this.aiService.parseAdjectives(text, this.users);

		// then
		assertEquals("[liebevoll, leidend, hoffnungsvoll]", summary.adjectives.get(this.romeo).toString());
		assertEquals("[🥰, 😔, ❤️]", summary.emojis.get(this.romeo).toString());
		assertEquals("[emotional, ambivalent, stark]", summary.adjectives.get(this.julia).toString());
		assertEquals("[😥, 😌, 💞]", summary.emojis.get(this.julia).toString());
		assertEquals("abc\n\ndef.\n########", summary.text);
	}

	@Test
	public void parseAdjectives_6() {
		// when
		final String text = "abc\n\ndef.\n\n"
				+ "**Romeo**: Liebevoll, leidend, hoffnungsvoll 🥰😔❤️\n"
				+ "**Julia:** Emotional, ambivalent, stark 😥😌💞";

		// when
		final AiSummary summary = this.aiService.parseAdjectives(text, this.users);

		// then
		assertEquals("[liebevoll, leidend, hoffnungsvoll]", summary.adjectives.get(this.romeo).toString());
		assertEquals("[🥰, 😔, ❤️]", summary.emojis.get(this.romeo).toString());
		assertEquals("[emotional, ambivalent, stark]", summary.adjectives.get(this.julia).toString());
		assertEquals("[😥, 😌, 💞]", summary.emojis.get(this.julia).toString());
		assertEquals("abc\n\ndef.", summary.text);
	}

	@Test
	public void parseAdjectives_7() {
		// when
		final String text = "abc\ndef.\n"
				+ "Julia: emotional, impulsiv, liebesbedürftig 😘💔😊\n"
				+ "RoMeo: sehnsüchtig, nachdenklich, hoffnungsvoll 🥺❤️😊";

		// when
		final AiSummary summary = this.aiService.parseAdjectives(text, this.users);

		// then
		assertEquals("[sehnsüchtig, nachdenklich, hoffnungsvoll]", summary.adjectives.get(this.romeo).toString());
		assertEquals("[🥺, ❤️, 😊]", summary.emojis.get(this.romeo).toString());
		assertEquals("[emotional, impulsiv, liebesbedürftig]", summary.adjectives.get(this.julia).toString());
		assertEquals("[😘, 💔, 😊]", summary.emojis.get(this.julia).toString());
		assertEquals("abc\ndef.", summary.text);
	}
}