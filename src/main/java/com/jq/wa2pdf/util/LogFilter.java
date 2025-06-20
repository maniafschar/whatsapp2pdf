package com.jq.wa2pdf.util;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.jq.wa2pdf.entity.Log;
import com.jq.wa2pdf.entity.Log.LogStatus;
import com.jq.wa2pdf.repository.Repository;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(1)
public class LogFilter implements Filter {
	public static final ThreadLocal<String> body = new ThreadLocal<>();

	@Autowired
	private Repository repository;

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
			throws IOException, ServletException {
		final ContentCachingRequestWrapper req = new ContentCachingRequestWrapper((HttpServletRequest) request);
		final HttpServletResponse res = (HttpServletResponse) response;
		body.set(null);
		final Log log = new Log();
		final boolean loggable = !"OPTIONS".equals(req.getMethod()) && !"/action/ping".equals(req.getRequestURI());
		if (loggable) {
			log.setUri(req.getRequestURI());
			log.setMethod(req.getMethod());
			if (req.getHeader("referer") != null) {
				log.setReferer(req.getHeader("referer"));
				if (log.getReferer().length() > 255)
					log.setReferer(log.getReferer().substring(0, 255));
			}
			log.setIp(sanatizeIp(req.getHeader("X-Forwarded-For")));
			log.setPort(req.getLocalPort());
			final String query = req.getQueryString();
			if (query != null) {
				if (query.contains("&_="))
					log.setQuery(URLDecoder.decode(query.substring(0, query.indexOf("&_=")),
							StandardCharsets.UTF_8.name()));
				else if (!query.startsWith("_="))
					log.setQuery(URLDecoder.decode(query, StandardCharsets.UTF_8.name()));
				if (log.getQuery() != null && log.getQuery().length() > 255)
					log.setQuery(log.getQuery().substring(0, 252) + "...");
			}
		}
		final long time = System.currentTimeMillis();
		try {
			chain.doFilter(req, res);
		} finally {
			log.setTime((int) (System.currentTimeMillis() - time));
			log.setStatus(LogStatus.get(res.getStatus()));
			log.setCreatedAt(new Timestamp(Instant.now().toEpochMilli() - log.getTime()));
			final byte[] b = req.getContentAsByteArray();
			if (b != null && b.length > 0)
				log.setBody(log.getBody() + "\n" + new String(b, StandardCharsets.UTF_8));
			final String s = body.get();
			if (s != null) {
				log.setBody(log.getBody() + "\n" + s);
				body.set(null);
			}
			try {
				repository.save(log);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	private String sanatizeIp(final String ip) {
		if (ip != null && ip.contains(","))
			return ip.substring(ip.lastIndexOf(",") + 1).trim();
		return ip;
	}
}