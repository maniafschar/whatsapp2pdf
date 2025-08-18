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
		private final String search;

		private AdminData(final String search, final List<Log> logs, final List<Ticket> tickets) {
			super();
			this.search = search;
			this.logs = logs;
			this.tickets = tickets;
		}

		public List<Log> getLogs() {
			return this.logs;
		}

		public List<Ticket> getTickets() {
			return this.tickets;
		}

		public String getSearch() {
			return this.search;
		}
	}

	public AdminData init() {
		final String search = "createdAt>cast('" + Instant.now().minus(Duration.ofDays(1)).toString().substring(0, 10)
				+ "' as timestamp) and uri not like '/sc/%'";
		return new AdminData(search,
				this.repository.list("from Log where " + search + " order by id desc", Log.class),
				this.repository.list("from Ticket where deleted=false order by id desc", Ticket.class));
	}

	public List<Log> log(final String search) {
		validateSearch(search);
		return this.repository.list("from Log where " + search + " order by id desc", Log.class);
	}

	public String build(final String type) throws IOException {
		final ProcessBuilder pb = new ProcessBuilder(
				"status".equals(type) ? new String[] { "/usr/bin/bash", "-c", "ps -eF|grep java" }
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

	private void validateSearch(final String search) {
		final StringBuilder s = new StringBuilder(search.toLowerCase());
		int p, p2;
		while ((p = s.indexOf("'")) > -1) {
			p2 = p;
			do {
				p2 = s.indexOf("'", p2 + 1);
			} while (p2 > 0 && "\\".equals(s.substring(p2 - 1, p2)));
			if (p2 < 0)
				throw new IllegalArgumentException(
						"Invalid quote in search: " + search);
			s.delete(p, p2 + 1);
		}
		if (s.indexOf(";") > -1 || s.indexOf("union") > -1 || s.indexOf("update") > -1
				|| s.indexOf("insert") > -1 || s.indexOf("delete") > -1)
			throw new IllegalArgumentException(
					"Invalid expression in search: " + search);
	}
}
