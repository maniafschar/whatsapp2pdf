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
			"🤷‍♂️ 1f937_200d_2642_fe0f",
			"🇦🇮 1f1e6_1f1ee",
			"🇰🇵 1f1f0_1f1f5",
			"🇰🇷 1f1f0_1f1f7",
			"🇫🇴 1f1eb_1f1f4",
			"🇶🇦 1f1f6_1f1e6",
			"🇧🇭 1f1e7_1f1ed",
			"🇧🇯 1f1e7_1f1ef",
			"🇧🇩 1f1e7_1f1e9",
			"🇻🇬 1f1fb_1f1ec",
			"🇧🇿 1f1e7_1f1ff",
			"🇦🇮 1f1e6_1f1ee",
			"🇺🇳 1f1fa_1f1f3",
			"🇵🇹 1f1f5_1f1f9",
			"🇩🇪 1f1e9_1f1ea",
			"🇹🇷 1f1f9_1f1f7",
			"🇧🇷 1f1e7_1f1f7",
			"🇯🇲 1f1ef_1f1f2",
			"🇯🇵 1f1ef_1f1f5",
			"🇮🇹 1f1ee_1f1f9",
			"🇧🇪 1f1e7_1f1ea",
			"🇭🇷 1f1ed_1f1f7",
			"🇱🇾 1f1f1_1f1fe",
			"🇨🇳 1f1e8_1f1f3",
			"🇨🇭 1f1e8_1f1ed",
			"🇩🇰 1f1e9_1f1f0",
			"🇨🇦 1f1e8_1f1e6",
			"🇫🇷 1f1eb_1f1f7",
			"🇮🇷 1f1ee_1f1f7",
			"🇨🇳 1f1e8_1f1f3",
			"🇨🇳 1f1e8_1f1f3",
			"🇷🇸 1f1f7_1f1f8",
			"🇷🇸 1f1f7_1f1f8",
			"🇷🇸 1f1f7_1f1f8",
			"🇷🇸 1f1f7_1f1f8"
	})
	void getEmojiId(final String emoji) {
		// given

		// when
		final String id = Utilities.emojiId(emoji.split(" ")[0]);

		// then
		assertEquals(emoji.split(" ")[1], id);
	}

}
