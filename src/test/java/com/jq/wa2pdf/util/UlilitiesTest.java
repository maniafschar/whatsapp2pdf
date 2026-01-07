package com.jq.wa2pdf.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class UlilitiesTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"🤯 1f92f",
			"❤️‍🔥 2764_fe0f_200d_1f525",
			"🤷‍♂️somebullshittext 1f937_200d_2642_fe0f",
			"🤷🏾‍♀️ 1f937_1f3fe_200d_2640_fe0f",
			"🤷‍♂️ 1f937_200d_2642_fe0f"
	})
	void getEmojiId(final String emoji) {
		// given

		// when
		final String id = Utilities.getEmojiId(emoji.split(" ")[0]);

		// then
		assertEquals(emoji.split(" ")[1], id);
	}

}
