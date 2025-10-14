package com.jq.wa2pdf.service;

import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.jq.wa2pdf.TestConfig;
import com.jq.wa2pdf.WhatsApp2PdfApplication;
import com.jq.wa2pdf.service.AdminService.AdminData;
import com.jq.wa2pdf.service.ExtractService.Attributes;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { WhatsApp2PdfApplication.class,
		TestConfig.class }, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
				"server.port=9001", "server.servlet.context-path=/rest" })
@ActiveProfiles("test")
public class PdfServiceTest {
	@Autowired
	private ExtractService extractService;

	@Autowired
	private PdfService pdfService;

	@Autowired
	private AdminService adminService;

	@Test
	public void create_1() throws Exception {
		this.test("Chat 1 WhatsApp con abc");
	}

	@Test
	public void create_2() throws Exception {
		this.test("Chat 2 de WhatsApp con ");
	}

	@Test
	public void create_3() throws Exception {
		this.test("Chat 3 WhatsApp - 9.14_D_CI_8_Pete_87_Germany_");
	}

	@Test
	public void create_4() throws Exception {
		this.test("Chat 4 WhatsApp - def");
	}

	@Test
	public void create_5() throws Exception {
		this.test("Chat 5 WhatsApp - Schafkopf");
	}

	@Test
	public void create_6() throws Exception {
		this.test("Chat 6 WhatsApp with +81 234543 3463");
	}

	@Test
	public void create_7() throws Exception {
		this.test("Chat 7 WhatsApp with Hab");
	}

	private void test(final String filename) throws Exception {
		// given
		final Attributes attributes = this.extractService.analyse(
				this.getClass().getResourceAsStream("/zip/" + filename + ".zip"), null,
				UUID.randomUUID().toString());

		// when
		this.pdfService.create(attributes.getId(), attributes.getPeriods().get(0).period,
				attributes.getUsers().get(0).user, null);

		// then
		this.assertCreation(attributes.getId(), attributes.getPeriods().get(0).period);
	}

	private void assertCreation(final String id, final String period) throws Exception {
		try{
			for (int i = 0; i < 20; i++) {
				Thread.sleep(500L);
				final Path path = this.pdfService.get(id, period);
				if (path != null) {
					final AdminData adminData = this.adminService.init();
					if (adminData.getTickets().size() > 0)
						throw new RuntimeException("PDF creation error\n" +
								adminData.getTickets().stream().map(e -> e.getNote()).collect(Collectors.joining("\n")));
					return;
				}
			}
			throw new RuntimeException("PDF creation timed out!");
		} finally {
			this.extractService.delete(id);
		}
	}
}
