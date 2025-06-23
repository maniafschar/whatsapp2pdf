package com.jq.wa2pdf.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
public class Log extends BaseEntity {
	private String method;
	private String body = "";
	private String referer;
	private String query;
	private String ip;
	private String uri;
	@Enumerated(EnumType.STRING)
	private LogStatus status;
	private int port;
	private int time;

	public enum LogStatus {
		ErrorAuthentication, ErrorClient, ErrorRedirection, ErrorServer, Ok;

		public static LogStatus get(int httpCode) {
			return httpCode < 300 ? Ok : httpCode < 400 ? ErrorRedirection : httpCode < 500 ? ErrorClient : ErrorServer;
		}
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(final String method) {
		this.method = method;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(final String query) {
		this.query = query;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(final String uri) {
		this.uri = uri;
	}

	public int getTime() {
		return time;
	}

	public void setTime(final int time) {
		this.time = time;
	}

	public LogStatus getStatus() {
		return status;
	}

	public void setStatus(final LogStatus status) {
		this.status = status;
	}

	public int getPort() {
		return port;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public String getBody() {
		return body;
	}

	public void setBody(final String body) {
		this.body = body;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(final String ip) {
		this.ip = ip;
	}

	public String getReferer() {
		return referer;
	}

	public void setReferer(final String referer) {
		this.referer = referer;
	}
}
