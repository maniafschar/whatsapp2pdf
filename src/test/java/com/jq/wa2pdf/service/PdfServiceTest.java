package com.jq.wa2pdf.service;

import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.jq.wa2pdf.TestConfig;
import com.jq.wa2pdf.WhatsApp2PdfApplication;
import com.jq.wa2pdf.entity.Ticket;
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

	@ParameterizedTest
	@ValueSource(strings = {
			"Chat 1 WhatsApp con abc",
			"Chat 2 de WhatsApp con ",
			"Chat 3 WhatsApp - 9.14_D_CI_8_Pete_87_Germany_",
			"Chat 4 WhatsApp - def",
			"Chat 5 WhatsApp - Schafkopf",
			"Chat 6 WhatsApp with +81 234543 3463",
			"Chat 7 WhatsApp with Hab",
			"Chat 8 Conversa do WhatsApp com A K",
			"Chat 9 WhatsApp den Za",
			"Chat 10 WhatsApp con RED UDR",
			"Chat 11 WhatsApp Chat"
	})
	void test(final String filename) throws Exception {
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
		try {
			for (int i = 0; i < 20; i++) {
				Thread.sleep(500L);
				final Path path = this.pdfService.get(id, period);
				if (path != null) {
					final AdminData adminData = this.adminService.init();
					final String errors = adminData.getTickets().stream()
							.filter(e -> e.getNote().startsWith(Ticket.ERROR)).map(e -> e.getNote())
							.collect(Collectors.joining("\n\n"));
					if (errors.length() > 0)
						throw new RuntimeException("PDF creation error\n" + errors);
					return;
				}
			}
			throw new RuntimeException("PDF creation timed out!");
		} finally {
			this.extractService.delete(id);
		}
	}
}