package com.jq.wa2pdf;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.jq.wa2pdf.service.AiService;
import com.jq.wa2pdf.util.Utilities;

@Profile("test")
@TestConfiguration
@EnableTransactionManagement
public class TestConfig {
	@Bean
	public DataSource getDataSource() throws Exception {
		final DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setUrl("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1");
		return dataSource;
	}

	@Service
	@Primary
	public class AiServiceMock extends AiService {
		@Override
		protected AiSummary summerizeGemini(final String text, final Set<String> users) {
			String json = "{\"summary\": \"The conversation between def and abc is characterized by a strong emotional connection and a lot of back-and-forth about their feelings, future plans, and daily lives. def expresses... abc, while reciprocating affection, often seems more reserved or cautious, sometimes overwhelmed by def's intensity. She expresses a desire... The world outside faded to a soft blur, replaced by the singular, burning focus on that moment, that feeling, that you. My heart hammered a frantic rhythm, a drumbeat against my ribs, a silent plea for your touch, your voice, your breath against my skin. It wasn't just a want; it was a physical ache, a void only you could fill, a hunger for the sweet surrender of your arms, the electric promise in your eyes. I dreamt of sun-warmed skin, the taste of salt on our lips, and laughter echoing under starlit skies, a symphony of shared joy and whispered confessions. Every shadow seemed to hold your shape, every breeze carried your scent, a constant, tantalizing reminder of what I craved. The world was a muted canvas, waiting for the vibrant strokes of your presence to bring it to life, a deep, undeniable pull towards a reality where our desires intertwined, becoming one breathtaking, undeniable force, a longing so profound it felt like the very essence of my being, a desperate, beautiful need for you to be here, now, forever.\",\"attributes\":[";
			final Iterator<String> it = users.iterator();
			while (it.hasNext()) {
				json += "{\"name\":\"" + it.next() + "\",\"adjectives\":["
						+ Utilities.createAdjectives(false).stream().collect(Collectors.joining("\",\"", "\"", "\""))
						+ "],\"emojis\":["
						+ Utilities.createAdjectives(true).stream().collect(Collectors.joining("\",\"", "\"", "\""))
						+ "]},";
			}
			final AiSummary aiSummary = this.convert(json.substring(0, json.length() - 1) + "]}", users);
			try {
				aiSummary.image = IOUtils.toByteArray(this.getClass().getResourceAsStream("/image/ai.png"));
			} catch (final IOException ex) {
				throw new RuntimeException(ex);
			}
			return aiSummary;
		}
	}
}