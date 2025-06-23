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

		private AdminData(final List<Log> logs, final List<Ticket> tickets) {
			super();
			this.logs = logs;
			this.tickets = tickets;

			public List<Log> getLogs() {
				return logs;
			}

			public List<Ticket> getTickets() {
				return tickets;
			}
		}
	}
  
	public String init() {
		return new AdminData(
				this.repository.list("from Log where createdAt>'" + Instant.now().minus(Duration.ofDays(5)).toString() + "'", Log.class),
				this.repository.list("from Ticket", Ticket.class));
	}
}
