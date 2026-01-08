package com.jq.wa2pdf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
		assertNotNull(summary);
		assertNotNull(summary.image);
		assertNotNull(summary.text);
		assertEquals(2, summary.adjectives.size());
		assertEquals(2, summary.emojis.size());
	}

	@Test
	public void convert_1() {
		// given
		final String text = "{\"summary\":\"yxz\\n########\\n\\n\",\"attributes\":["
				+ "{\"name\":\"ROmeo\",\"adjectives\":[\"Assertive\",\"guarded\",\"playful\"],\"emojis\":[\"😉\",\"😁\",\"👌\"]},"
				+ "{\"name\":\"Julia\",\"adjectives\":[\"Persistent\",\"longing\",\"provocative\"],\"emojis\":[\"🎉\",\"🏋️\",\"🥵\"]}"
				+ "]}";

		// when
		final AiSummary summary = this.aiService.convert(text, this.users);

		// then
		assertEquals("[assertive, guarded, playful]", summary.adjectives.get(this.romeo).toString());
		assertEquals("[😉, 😁, 👌]", summary.emojis.get(this.romeo).toString());
		assertEquals("[persistent, longing, provocative]", summary.adjectives.get(this.julia).toString());
		assertEquals("[🎉, 🏋️, 🥵]", summary.emojis.get(this.julia).toString());
		assertEquals("yxz\n########", summary.text);
	}

	@Test
	public void convert_2() {
		// when
		final String text = "{\"summary\":\"abc\\n\\ndef.\\n########\\n\\n\",\"attributes\":["
				+ "{\"name\":\"Julia\",\"adjectives\":[\"Gefühlvoll\",\"Loyal\",\"Stark\"],\"emojis\":[\"🎉\",\"🏋️\",\"🥵\"]},"
				+ "{\"name\":\"ROmeo\",\"adjectives\":[\"Leidenschaftlich\",\"Emotional\",\"Unsicher\"],\"emojis\":[\"😉\",\"😁\",\"👌\"]}"
				+ "]}";

		// when
		final AiSummary summary = this.aiService.convert(text, this.users);

		// then
		assertEquals("[leidenschaftlich, emotional, unsicher]", summary.adjectives.get(this.romeo).toString());
		assertEquals("[😉, 😁, 👌]", summary.emojis.get(this.romeo).toString());
		assertEquals("[gefühlvoll, loyal, stark]", summary.adjectives.get(this.julia).toString());
		assertEquals("[🎉, 🏋\ufe0f, 🥵]", summary.emojis.get(this.julia).toString());
		assertEquals("abc\n\ndef.\n########", summary.text);
	}

	@Test
	public void convert_3() {
		// when
		final String text = "{\"summary\":\"abc\\n\\ndef.\\n########\\n\",\"attributes\":["
				+ "{\"name\":\"Romeo\",\"adjectives\":[\"leidenschaftlich\",\"sehnsüchtig\",\"kämpferisch\"],\"emojis\":[\"❤️\",\"😘\",\"🔥\"]},"
				+ "{\"name\":\"Julia\",\"adjectives\":[\"zärtlich\",\"emotional\",\"unsicher\"],\"emojis\":[\"😘\",\"🥺\",\"💖\"]}"
				+ "]}";
		final String name = this.julia + " Klöckner";
		this.users.remove(this.julia);
		this.users.add(name);

		// when
		final AiSummary summary = this.aiService.convert(text, this.users);

		// then
		assertEquals("[leidenschaftlich, sehnsüchtig, kämpferisch]", summary.adjectives.get(this.romeo).toString());
		assertEquals("[❤️, 😘, 🔥]", summary.emojis.get(this.romeo).toString());
		assertEquals("[zärtlich, emotional, unsicher]", summary.adjectives.get(name).toString());
		assertEquals("[😘, 🥺, 💖]", summary.emojis.get(name).toString());
		assertEquals("abc\n\ndef.\n########", summary.text);
	}

	@Test
	public void convert_4() {
		// when
		final String text = "{\"summary\":\"abc\\n\\ndef.\\n########\\n\",\"attributes\":["
				+ "{\"name\":\"Julia\",\"adjectives\":[\"**emotional**\",\"leidenschaftlich\",\"**nachdenklich**\"],\"emojis\":[\"❤️\",\"😘\",\"🔥\"]},"
				+ "{\"name\":\"Romeo\",\"adjectives\":[\"**sehnsüchtig**\",\"aufgewühlt\",\"**aufrichtig**\"],\"emojis\":[\"😘\",\"🥺\",\"💖\"]}"
				+ "]}";

		// when
		final AiSummary summary = this.aiService.convert(text, this.users);

		// then
		assertEquals("[sehnsüchtig, aufgewühlt, aufrichtig]", summary.adjectives.get(this.romeo).toString());
		assertEquals("[😘, 🥺, 💖]", summary.emojis.get(this.romeo).toString());
		assertEquals("[emotional, leidenschaftlich, nachdenklich]", summary.adjectives.get(this.julia).toString());
		assertEquals("[❤️, 😘, 🔥]", summary.emojis.get(this.julia).toString());
		assertEquals("abc\n\ndef.\n########", summary.text);
	}

	@Test
	public void convert_5() {
		// when
		final String text = "{\"summary\":\"abc\\n\\ndef.\\n########\\n\",\"attributes\":["
				+ "{\"name\":\"RoMeo\",\"adjectives\":[\"Liebevoll\",\"leidend\",\"hoffnungsvoll\"],\"emojis\":[\"🥰\",\"😔\",\"❤️\"]},"
				+ "{\"name\":\"Julia\",\"adjectives\":[\"Emotional\",\"ambivalent\",\"stark\"],\"emojis\":[\"😥\",\"😌\",\"💞\"]}"
				+ "]}";

		// when
		final AiSummary summary = this.aiService.convert(text, this.users);

		// then
		assertEquals("[liebevoll, leidend, hoffnungsvoll]", summary.adjectives.get(this.romeo).toString());
		assertEquals("[🥰, 😔, ❤️]", summary.emojis.get(this.romeo).toString());
		assertEquals("[emotional, ambivalent, stark]", summary.adjectives.get(this.julia).toString());
		assertEquals("[😥, 😌, 💞]", summary.emojis.get(this.julia).toString());
		assertEquals("abc\n\ndef.\n########", summary.text);
	}

	@Test
	public void convert_6() {
		// when
		final String text = "{\"summary\":\"abc\\n\\ndef.\\n\\n\",\"attributes\":["
				+ "{\"name\":\"**Romeo**\",\"adjectives\":[\"Liebevoll\",\"leidend\",\"hoffnungsvoll\"],\"emojis\":[\"🥰\",\"😔\",\"❤️\"]},"
				+ "{\"name\":\"**Julia:**\",\"adjectives\":[\"Emotional\",\"ambivalent\",\"stark\"],\"emojis\":[\"😥\",\"😌\",\"💞\"]}"
				+ "]}";

		// when
		final AiSummary summary = this.aiService.convert(text, this.users);

		// then
		assertEquals("[liebevoll, leidend, hoffnungsvoll]", summary.adjectives.get(this.romeo).toString());
		assertEquals("[🥰, 😔, ❤️]", summary.emojis.get(this.romeo).toString());
		assertEquals("[emotional, ambivalent, stark]", summary.adjectives.get(this.julia).toString());
		assertEquals("[😥, 😌, 💞]", summary.emojis.get(this.julia).toString());
		assertEquals("abc\n\ndef.", summary.text);
	}

	@Test
	public void convert_7() {
		// when
		final String text = "{\"summary\":\"abc\\ndef.\",\"attributes\":["
				+ "{\"name\":\"Julia\",\"adjectives\":[\"emotional\",\"impulsiv\",\"liebesbedürftig\"],\"emojis\":[\"😘\",\"💔\",\"😊\"]},"
				+ "{\"name\":\"RoMeo\",\"adjectives\":[\"sehnsüchtig\",\"nachdenklich\",\"hoffnungsvoll\"],\"emojis\":[\"🥺\",\"❤️\",\"😊\"]}"
				+ "]}";

		// when
		final AiSummary summary = this.aiService.convert(text, this.users);

		// then
		assertEquals("[sehnsüchtig, nachdenklich, hoffnungsvoll]", summary.adjectives.get(this.romeo).toString());
		assertEquals("[🥺, ❤️, 😊]", summary.emojis.get(this.romeo).toString());
		assertEquals("[emotional, impulsiv, liebesbedürftig]", summary.adjectives.get(this.julia).toString());
		assertEquals("[😘, 💔, 😊]", summary.emojis.get(this.julia).toString());
		assertEquals("abc\ndef.", summary.text);
	}
}