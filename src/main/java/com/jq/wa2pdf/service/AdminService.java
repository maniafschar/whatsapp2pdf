package com.jq.wa2pdf.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jq.wa2pdf.entity.Log;
import com.jq.wa2pdf.entity.Ticket;
import com.jq.wa2pdf.repository.Repository;

@Service
public class AdminService {
	@Autowired
	private Repository repository;

	public static class AdminData {
		private final List<Log> logs;
		private final List<Ticket> tickets;
		private final String tempDir;

		private AdminData(final List<Log> logs, final List<Ticket> tickets, final String tempDir) {
			super();
			this.logs = logs;
			this.tickets = tickets;
			this.tempDir = tempDir;
		}

		public List<Log> getLogs() {
			return this.logs;
		}

		public List<Ticket> getTickets() {
			return this.tickets;
		}

		public String getTempDir() {
			return this.tempDir;
		}
	}

	public AdminData init() {
		return new AdminData(
				this.repository.list(
						"from Log where createdAt>cast('" + Instant.now().minus(Duration.ofDays(5)).toString()
								+ "' as timestamp)",
						Log.class),
				this.repository.list("from Ticket", Ticket.class),
				ExtractService.getTempDir("").toAbsolutePath().toFile().getAbsolutePath());
	}

	public void createTicket(final Ticket ticket) {
		if (this.repository
				.list("from Ticket where note like '" + ticket.getNote().replaceAll("\n", "_") + "'", Ticket.class)
				.size() == 0)
			this.repository.save(ticket);
	}
}