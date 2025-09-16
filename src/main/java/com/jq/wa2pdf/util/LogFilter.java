package com.jq.wa2pdf.util;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Enumeration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.jq.wa2pdf.entity.Log;
import com.jq.wa2pdf.entity.Ticket;
import com.jq.wa2pdf.repository.Repository;
import com.jq.wa2pdf.service.AdminService;

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
	@Autowired
	private AdminService adminService;

	@Autowired
	private Repository repository;

	@Value("${app.supportCenter.secret}")
	private String supportCenterSecret;

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
			throws IOException, ServletException {
		final ContentCachingRequestWrapper req = new ContentCachingRequestWrapper((HttpServletRequest) request);
		final ContentCachingResponseWrapper res = new ContentCachingResponseWrapper((HttpServletResponse) response);
		final Log log = new Log();
		log.setUri(req.getRequestURI());
		log.setMethod(req.getMethod());
		if (req.getHeader("referer") != null)
			log.setReferer(req.getHeader("referer"));
		log.setIp(this.sanatizeIp(req.getHeader("X-Forwarded-For")));
		if ("".equals(log.getIp()))
			log.setIp(request.getRemoteAddr());
		log.setPort(req.getLocalPort());
		final String query = req.getQueryString();
		if (query != null) {
			if (query.contains("&_="))
				log.setQuery(URLDecoder.decode(query.substring(0, query.indexOf("&_=")),
						StandardCharsets.UTF_8.name()));
			else if (!query.startsWith("_="))
				log.setQuery(URLDecoder.decode(query, StandardCharsets.UTF_8.name()));
		}
		final long time = System.currentTimeMillis();
		try {
			if ("OPTIONS".equals(req.getMethod()) || !req.getRequestURI().startsWith("/sc/")
					|| this.supportCenterSecret.equals(req.getHeader("user")))
				chain.doFilter(req, res);
			else {
				log.setBody("unauthorized acccess:\n" + req.getRequestURI() + "\n" + req.getHeader("user"));
				log.setStatus(HttpStatus.UNAUTHORIZED.value());
			}
		} finally {
			log.setTime((int) (System.currentTimeMillis() - time));
			if (log.getStatus() == 0)
				log.setStatus(res.getStatus());
			log.setCreatedAt(new Timestamp(Instant.now().toEpochMilli() - log.getTime()));
			byte[] b = req.getContentAsByteArray();
			if (b != null && b.length > 0)
				log.setBody((log.getBody() + '\n' + new String(b, StandardCharsets.UTF_8).trim()));
			b = res.getContentAsByteArray();
			if (b != null && b.length > 0)
				log.setBody((log.getBody() + '\n' + new String(b, StandardCharsets.UTF_8).trim()));
			res.copyBodyToResponse();
			try {
				this.repository.save(log);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	private String sanatizeIp(final String ip) {
		if (ip == null)
			return "";
		if (ip.contains(","))
			return ip.substring(ip.lastIndexOf(',') + 1).trim();
		return ip;
	}
}
