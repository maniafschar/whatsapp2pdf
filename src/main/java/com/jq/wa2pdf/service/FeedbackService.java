package com.jq.wa2pdf.service;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.ImageHtmlEmail;
import org.apache.commons.mail.resolver.DataSourceUrlResolver;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jq.wa2pdf.entity.Feedback;

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

	@Value("${app.mail.password}")
	private String emailPassword;

	public String save(final Feedback feedback) {
		repository.save(feedback);
		sendEmail(feedback);
		return "An email has been sent to you. Please confirm to publish your feedback.";
	}

	public Feedback one(final BigInteger id, final String pin) {
		final List<Feedback> list = repository.list("");
		return list.size() == 1 ? list.get(0) : null;
	}

	public void list() {
		final List<Feedback> list = repository.list("");
		return list.size() == 1 ? list.get(0) : null;
	}

	private void sendEmail(final Feedback feedback) {
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
