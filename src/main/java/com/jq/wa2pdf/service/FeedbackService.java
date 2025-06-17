package com.jq.wa2pdf.service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.ImageHtmlEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.jq.wa2pdf.entity.Feedback;
import com.jq.wa2pdf.repository.Repository;

@Service
public class FeedbackService {
	@Autowired
	private Repository repository;

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

	public String save(final String id, final Feedback feedback) throws EmailException {
		if (Files.exists(ExtractService.getTempDir(id))) {
			repository.save(feedback);
			sendEmail(feedback);
			return "An email has been sent to you. Please confirm to publish your feedback.";
		}
		return "You data has already been deleted. Please leave feedback before deleting you data.";
	}

	public Feedback one(final BigInteger id, final String pin) {
		@SuppressWarnings("unchecked")
		final List<Feedback> list = (List<Feedback>) repository.list("");
		return list.size() == 1 ? list.get(0) : null;
	}

	@SuppressWarnings("unchecked")
	public List<Feedback> list() {
		return (List<Feedback>) repository.list("");
	}

	private void sendEmail(final Feedback feedback) throws EmailException {
		final ImageHtmlEmail email = mailCreateor.create();
		email.setHostName(emailHost);
		email.setSmtpPort(emailPort);
		email.setCharset(StandardCharsets.UTF_8.name());
		email.setAuthenticator(new DefaultAuthenticator(emailAddress, emailPassword));
		email.setSSLOnConnect(true);
		email.setFrom(emailAddress, "WhatsApp PDF Converter");
		email.addTo(feedback.getEmail());
		email.setSubject("");
		email.setTextMsg("");
		email.setHtmlMsg("");
		email.send();
	}

	@Component
	public static class MailCreateor {
		public ImageHtmlEmail create() {
			return new ImageHtmlEmail();
		}
	}
}
