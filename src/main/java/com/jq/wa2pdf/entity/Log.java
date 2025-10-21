package com.jq.wa2pdf.entity;

import org.springframework.http.HttpStatus;

import com.jq.wa2pdf.api.ApplicationApi;
import com.jq.wa2pdf.util.Utilities;

import jakarta.persistence.Entity;

@Entity
public class Log extends BaseEntity {
	private String method = "";
	private String body = "";
	private String referer = "";
	private String query = "";
	private String ip = "";
	private String uri = "";
	private int status;
	private int port;
	private int time;

	public enum LogStatus {
		ErrorAuthentication, ErrorClient, ErrorRedirection, ErrorServer, Ok;

		private static LogStatus map(final int status) {
			return status < 300 || status == ApplicationApi.STATUS_PROCESSING_PDF ? Ok
					: status == HttpStatus.UNAUTHORIZED.value() ? ErrorAuthentication
							: status < 400 ? ErrorRedirection : status < 500 ? ErrorClient : ErrorServer;
		}
	}

	public String getMethod() {
		return this.method;
	}

	public void setMethod(final String method) {
		this.method = method;
	}

	public String getQuery() {
		return this.query;
	}

	public void setQuery(final String query) {
		this.query = query;
	}

	public String getUri() {
		return this.uri;
	}

	public void setUri(final String uri) {
		this.uri = uri;
	}

	public int getTime() {
		return this.time;
	}

	public void setTime(final int time) {
		this.time = time;
	}

	public int getStatus() {
		return this.status;
	}

	public void setStatus(final int status) {
		this.status = status;
	}

	public LogStatus getLogStatus() {
		return LogStatus.map(this.status);
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public String getBody() {
		return this.body;
	}

	public void setBody(final String body) {
		this.body = Utilities.trim(body, 255);
	}

	public String getIp() {
		return this.ip;
	}

	public void setIp(final String ip) {
		this.ip = ip;
	}

	public String getReferer() {
		return this.referer;
	}

	public void setReferer(final String referer) {
		this.referer = Utilities.trim(referer, 255);
	}
}
