package com.jq.wa2pdf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.jq.wa2pdf.TestConfig;
import com.jq.wa2pdf.WhatsApp2PdfApplication;
import com.jq.wa2pdf.service.ExtractService.Attributes;

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
	public void analyse_1() throws Exception {
		// given
		// 20/05/25, 13:49 - abc:

		// when
		final Attributes attributes = this.extractService.analyse(
				this.getClass().getResourceAsStream("/zip/Chat 1 WhatsApp con abc.zip"), null,
				UUID.randomUUID().toString());

		// then
		assertNotNull(attributes);
		assertEquals("d/M/yy", attributes.getDateFormat());
		assertEquals(2, attributes.getPeriods().size());
		assertEquals("\\d/05/25", attributes.getPeriods().get(0).period);
		assertEquals("\\d/06/25", attributes.getPeriods().get(1).period);
		assertEquals(2, attributes.getUsers().size());
		assertEquals("abc", attributes.getUsers().get(0).user);
		assertEquals("def", attributes.getUsers().get(1).user);
		this.extractService.delete(attributes.getId());
	}

	@Test
	public void analyse_2() throws Exception {
		// given
		// 30/7/2017, 12:33 - ‎abc:

		// when
		final Attributes attributes = this.extractService.analyse(
				this.getClass().getResourceAsStream("/zip/Chat 2 de WhatsApp con .zip"), null,
				UUID.randomUUID().toString());

		// then
		assertNotNull(attributes);
		assertEquals("d/M/yyyy", attributes.getDateFormat());
		assertEquals(3, attributes.getPeriods().size());
		assertEquals("\\d/7/2017", attributes.getPeriods().get(0).period);
		assertEquals("\\d/8/2017", attributes.getPeriods().get(1).period);
		assertEquals("\\d/9/2017", attributes.getPeriods().get(2).period);
		assertEquals(5, attributes.getUsers().size());
		assertEquals("abc", attributes.getUsers().get(0).user);
		assertEquals("deñ", attributes.getUsers().get(1).user);
		assertEquals("gh i", attributes.getUsers().get(2).user);
		assertEquals("jkl", attributes.getUsers().get(3).user);
		assertEquals("mn", attributes.getUsers().get(4).user);
		this.extractService.delete(attributes.getId());
	}

	@Test
	public void analyse_3() throws Exception {
		// given
		// [23/08/2025, 12:15:10 AM] abc:

		// when
		final Attributes attributes = this.extractService.analyse(
				this.getClass().getResourceAsStream("/zip/Chat 3 WhatsApp - 9.14_D_CI_8_Pete_87_Germany_.zip"), null,
				UUID.randomUUID().toString());

		// then
		assertNotNull(attributes);
		assertEquals("d/M/yyyy", attributes.getDateFormat());
		assertEquals(1, attributes.getPeriods().size());
		assertEquals("\\d/08/2025", attributes.getPeriods().get(0).period);
		assertEquals(2, attributes.getUsers().size());
		assertEquals("Pete", attributes.getUsers().get(0).user);
		assertEquals("Karen", attributes.getUsers().get(1).user);
		this.extractService.delete(attributes.getId());
	}

	@Test
	public void analyse_4() throws Exception {
		// given
		// [22.01.25, 16:31:54] def: <200e>Nachrichten

		// when
		final Attributes attributes = this.extractService.analyse(
				this.getClass().getResourceAsStream("/zip/Chat 4 WhatsApp - def.zip"), null,
				UUID.randomUUID().toString());

		// then
		assertNotNull(attributes);
		assertEquals("d.M.yy", attributes.getDateFormat());
		assertEquals(1, attributes.getPeriods().size());
		assertEquals("\\d.01.25", attributes.getPeriods().get(0).period);
		assertEquals(2, attributes.getUsers().size());
		assertEquals("def", attributes.getUsers().get(0).user);
		assertEquals("abc", attributes.getUsers().get(1).user);
		this.extractService.delete(attributes.getId());
	}

	@Test
	public void analyse_5() throws Exception {
		// given
		// [27.08.24, 10:48:34] abc:‎

		// when
		final Attributes attributes = this.extractService.analyse(
				this.getClass().getResourceAsStream("/zip/Chat 5 WhatsApp - Schafkopf.zip"), null,
				UUID.randomUUID().toString());

		// then
		assertNotNull(attributes);
		assertEquals("d.M.yy", attributes.getDateFormat());
		assertEquals(11, attributes.getPeriods().size());
		assertEquals("\\d.08.24", attributes.getPeriods().get(0).period);
		assertEquals("\\d.09.24", attributes.getPeriods().get(1).period);
		assertEquals("\\d.10.24", attributes.getPeriods().get(2).period);
		assertEquals("\\d.11.24", attributes.getPeriods().get(3).period);
		assertEquals("\\d.12.24", attributes.getPeriods().get(4).period);
		assertEquals("\\d.01.25", attributes.getPeriods().get(5).period);
		assertEquals("\\d.02.25", attributes.getPeriods().get(6).period);
		assertEquals("\\d.03.25", attributes.getPeriods().get(7).period);
		assertEquals("\\d.04.25", attributes.getPeriods().get(8).period);
		assertEquals("\\d.05.25", attributes.getPeriods().get(9).period);
		assertEquals("\\d.06.25", attributes.getPeriods().get(10).period);
		assertEquals(15, attributes.getUsers().size());
		assertEquals("Schafkopf", attributes.getUsers().get(0).user);
		assertEquals("Dieter Hackl", attributes.getUsers().get(1).user);
		assertEquals("Monika Heinzinger", attributes.getUsers().get(2).user);
		assertEquals("Josef Schmalzbauer", attributes.getUsers().get(3).user);
		assertEquals("Eberhard Seickel", attributes.getUsers().get(4).user);
		assertEquals("Lilo Kraml", attributes.getUsers().get(5).user);
		assertEquals("man", attributes.getUsers().get(6).user);
		assertEquals("Petra Feucht", attributes.getUsers().get(7).user);
		assertEquals("Sascha", attributes.getUsers().get(8).user);
		assertEquals("Marlis Kainhuber", attributes.getUsers().get(9).user);
		assertEquals("Markus Hüttl", attributes.getUsers().get(10).user);
		assertEquals("Barbara Seickel", attributes.getUsers().get(11).user);
		assertEquals("Suzanne Heureuse", attributes.getUsers().get(12).user);
		assertEquals("Du", attributes.getUsers().get(13).user);
		assertEquals("Monika Roitner", attributes.getUsers().get(14).user);
		this.extractService.delete(attributes.getId());
	}

	@Test
	public void analyse_6() throws Exception {
		// given
		// 14/04/2024, 9:10 am - Messages

		// when
		final Attributes attributes = this.extractService.analyse(
				this.getClass().getResourceAsStream("/zip/Chat 6 WhatsApp with +81 234543 3463.zip"), null,
				UUID.randomUUID().toString());

		// then
		assertNotNull(attributes);
		assertEquals("d/M/yyyy", attributes.getDateFormat());
		assertEquals(2, attributes.getPeriods().size());
		assertEquals("\\d/04/2024", attributes.getPeriods().get(0).period);
		assertEquals("\\d/06/2024", attributes.getPeriods().get(1).period);
		assertEquals(2, attributes.getUsers().size());
		assertEquals("abc", attributes.getUsers().get(0).user);
		assertEquals("def", attributes.getUsers().get(1).user);
		this.extractService.delete(attributes.getId());
	}

	@Test
	public void analyse_7() throws Exception {
		// given
		// 8/21/25, 11:18 PM - Nachrichten

		// when
		final Attributes attributes = this.extractService.analyse(
				this.getClass().getResourceAsStream("/zip/Chat 7 WhatsApp with Hab.zip"), null,
				UUID.randomUUID().toString());

		// then
		assertNotNull(attributes);
		assertEquals("M/d/yy", attributes.getDateFormat());
		assertEquals(2, attributes.getPeriods().size());
		assertEquals("8/\\d/25", attributes.getPeriods().get(0).period);
		assertEquals("9/\\d/25", attributes.getPeriods().get(1).period);
		assertEquals(2, attributes.getUsers().size());
		assertEquals("abc", attributes.getUsers().get(0).user);
		assertEquals("def", attributes.getUsers().get(1).user);
		this.extractService.delete(attributes.getId());
	}
}