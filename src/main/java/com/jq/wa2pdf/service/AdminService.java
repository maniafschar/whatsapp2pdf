package com.jq.wa2pdf.service;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.jq.wa2pdf.entity.Log;
import com.jq.wa2pdf.entity.Ticket;
import com.jq.wa2pdf.repository.Repository;

@Service
public class AdminService {
	@Autowired
	private Repository repository;

	@Value("${app.admin.buildScript}")
	private String buildScript;

	public static class AdminData {
		private final List<Log> logs;
		private final List<Ticket> tickets;

		private AdminData(final List<Log> logs, final List<Ticket> tickets) {
			super();
			this.logs = logs;
			this.tickets = tickets;
		}

		public List<Log> getLogs() {
			return this.logs;
		}

		public List<Ticket> getTickets() {
			return this.tickets;
		}
	}

	public AdminData init() {
		return new AdminData(
				this.repository.list(
						"from Log where createdAt>cast('" + Instant.now().minus(Duration.ofDays(5)).toString()
								+ "' as timestamp) and uri<>'/sc/init' order by id desc",
						Log.class),
				this.repository.list("from Ticket where deleted=false order by id desc", Ticket.class));
	}

	public String build(final String type) throws IOException {
		final ProcessBuilder pb = new ProcessBuilder(
				"status".equals(type) ? new String[] { "/usr/bin/bash", "-c", "ps aux|grep java" }
						: this.buildScript.replace("{type}", type).split(" "));
		pb.redirectErrorStream(true);
		return IOUtils.toString(pb.start().getInputStream(), StandardCharsets.UTF_8);
	}

	public void deleteTicket(final BigInteger id) {
		final Ticket ticket = this.repository.one(Ticket.class, id);
		ticket.setDeleted(true);
		this.repository.save(ticket);
	}

	public void createTicket(final Ticket ticket) {
		if (this.repository
				.list("from Ticket where note like '" + ticket.getNote().replaceAll("\n", "_") + "'", Ticket.class)
				.size() == 0)
			this.repository.save(ticket);
	}
}
