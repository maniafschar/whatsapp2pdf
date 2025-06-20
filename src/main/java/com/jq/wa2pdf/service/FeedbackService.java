package com.jq.wa2pdf.service;

import java.math.BigInteger;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.mail.EmailException;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jq.wa2pdf.entity.Feedback;
import com.jq.wa2pdf.repository.Repository;

@Service
public class FeedbackService {
	@Autowired
	private Repository repository;

	@Autowired
	private EmailService emailService;

	public String save(final String id, final Feedback feedback) throws EmailException {
		if (Strings.isEmpty(feedback.getName()) || Strings.isEmpty(feedback.getEmail())
				|| Strings.isEmpty(feedback.getNote()))
			return "No input.";
		if (Files.exists(ExtractService.getTempDir(id))) {
			if (feedback.getPin() != null
					&& !feedback.getPin().equals(repository.one(Feedback.class, feedback.getId()).getPin()))
				return "Pin expired.";
			feedback.setPin(generatePin(6));
			repository.save(feedback);
			emailService.send(feedback.getEmail(), "ydfgdf");
			return "An email has been sent to you. Please confirm to publish your feedback.";
		}
		return "You data has already been deleted. Please leave feedback before deleting you data.";
	}

	public Feedback one(final BigInteger id, final String pin) {
		return repository.one(Feedback.class, id);
	}

	@SuppressWarnings("unchecked")
	public List<Feedback> list() {
		return (List<Feedback>) repository.list("from Feedback f ORDER BY f.createdAt DESC");
	}

	private String generatePin(final int length) {
		final StringBuilder s = new StringBuilder();
		char c;
		while (s.length() < length) {
			c = (char) (Math.random() * 150);
			if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
				s.append(c);
		}
		return s.toString();
	}
}