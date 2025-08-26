package com.jq.wa2pdf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

public class PdfServiceTest {
	private final Pattern start = Pattern.compile("^.?\\[[0-9/:-\\\\,\\\\. (|\u202fAM|\u202fPM)]+\\] ([^:].*?)");

	@Test
	public void regex() {
		// given
		final String text = "[22.01.25, 16:31:54] Osagie Ogbomo: ‎Nachrichten und Anrufe sind Ende-zu-Ende-verschlüsselt. Nur Personen in diesem Chat können sie lesen, anhören oder teilen.";

		// when
		final boolean match = this.start.matcher(text).matches();

		// then
		assertTrue(match);
	}

	@Test
	public void regex2() {
		// given
		final String text = "[23/08/2025, 1:53:45 AM] 8.92/B/OC/3/Peter/63/Poland: ‎Messages and calls are end-to-end encrypted. Only people in this chat can read, listen to, or share them.";

		// when
		final boolean match = this.start.matcher(text).matches();

		// then
		assertTrue(match);
	}

	@Test
	public void regex3() {
		// given
		final String text = "<attached: 00000064-PHOTO-2015-09-29-01-06-36.jpg>";
		final Pattern p = Pattern.compile(
				".?<[a-zA-Z]{4,10}: [0-9]{8}-[A-Z]{4,10}-[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}\\.[a-z0-9]{3,4}>");

		// when
		final boolean match = p.matcher(text).matches();

		// then
		assertTrue(match);
	}

	@Test
	public void regex4() {
		// given
		final String text = "<Questo messaggio è stato modificato>";
		final Pattern p = Pattern.compile(".*[0-9\\.\\\\/\\?\\!\"'+\\-_=&%#@\\,:;\\{}\\()\\[\\]].*");

		// when
		final boolean match = p.matcher(text).matches();

		// then
		assertFalse(match);
	}

	@Test
	public void regex_false() {
		// given
		final String text = "Die braucht Nachhilfe. In allen Bereichen …";

		// when
		final boolean match = this.start.matcher(text).matches();

		// then
		assertFalse(match);
	}

	@Test
	public void regex_falseBlank() {
		// given
		final String text = "";

		// when
		final boolean match = this.start.matcher(text).matches();

		// then
		assertFalse(match);
	}

	@Test
	public void emoji() {
		// given
		final String emojiString = "🤦";

		// when
		final Emoji emoji = EmojiManager.getByUnicode(emojiString);

		// then
		assertNotNull(emoji);
		assertEquals(1, emoji.getAliases().size());
	}
}
