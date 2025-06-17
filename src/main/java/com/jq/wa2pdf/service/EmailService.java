package com.jq.wa2pdf.service;

import java.nio.charset.StandardCharsets;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.ImageHtmlEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
	@Autowired
	private MailCreateor mailCreateor;

	@Value("${app.mail.host}")
	private String emailHost;

	@Value("${app.mail.port}")
	private int emailPort;

	@Value("${app.mail.address}")
	private String emailAddress;

	@Value("${app.mail.password}")
	private String emailPassword;

	public void send(final String address, final String text) throws EmailException {
		final ImageHtmlEmail email = mailCreateor.create();
		email.setHostName(emailHost);
		email.setSmtpPort(emailPort);
		email.setCharset(StandardCharsets.UTF_8.name());
		email.setAuthenticator(new DefaultAuthenticator(emailAddress, emailPassword));
		email.setSSLOnConnect(true);
		email.setFrom(emailAddress, "WhatsApp PDF Converter");
		email.addTo(address);
		email.setSubject("Feedback on WhatsApp PDF Converter");
		email.setTextMsg(text);
		email.send();
	}

	@Component
	public static class MailCreateor {
		public ImageHtmlEmail create() {
			return new ImageHtmlEmail();
		}
	}
}
