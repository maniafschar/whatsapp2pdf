package com.jq.wa2pdf.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class PdfServiceTest {
	@Test
	public void regex() {
		// given
		final String text = "[22.01.25, 16:31:54] Osagie Ogbomo: ‎Nachrichten und Anrufe sind Ende-zu-Ende-verschlüsselt. Nur Personen in diesem Chat können sie lesen, anhören oder teilen.";
		final Pattern start = Pattern.compile("^.?\\[\\d\\d.\\d\\d.\\d\\d, \\d\\d:\\d\\d:\\d\\d\\] ([^:].*?)");

		// when
		final boolean match = start.matcher(text).matches();

		// then
		assertTrue(match);
	}

	@Test
	public void regex_false() {
		// given
		final String text = "Die braucht Nachhilfe. In allen Bereichen …";
		final Pattern start = Pattern.compile("^.?\\[\\d\\d.\\d\\d.\\d\\d, \\d\\d:\\d\\d:\\d\\d\\] ([^:].*?)");

		// when
		final boolean match = start.matcher(text).matches();

		// then
		assertFalse(match);
	}

	@Test
	public void regex_falseBlank() {
		// given
		final String text = "";
		final Pattern start = Pattern.compile("^.?\\[\\d\\d.\\d\\d.\\d\\d, \\d\\d:\\d\\d:\\d\\d\\] ([^:].*?)");

		// when
		final boolean match = start.matcher(text).matches();

		// then
		assertFalse(match);
	}
}
