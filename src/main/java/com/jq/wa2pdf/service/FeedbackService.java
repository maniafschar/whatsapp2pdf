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

	public String save(final String id, Feedback feedback) throws EmailException {
		if (Strings.isEmpty(feedback.getName()) || Strings.isEmpty(feedback.getEmail())
				|| Strings.isEmpty(feedback.getNote()))
			return "No input.";
		if (Files.exists(ExtractService.getTempDir(id))) {
			final boolean isNew = feedback.getId() == null;
			if (!isNew) {
				final Feedback original = repository.one(Feedback.class, feedback.getId());
				if (!original.getPin().equals(feedback.getPin()))
					return "Pin expired.";
				if (!Strings.isEmpty(feedback.getNote()))
					original.setNote(feedback.getNote());
				feedback = original;
				feedback.setVerified(true);
			}
			feedback.setPin(generatePin(6));
			repository.save(feedback);
			emailService.send(feedback.getEmail(), "?id=" + feedback.getId() + "&pin=" + feedback.getPin());
			return isNew ? "An email has been sent to you. Please confirm to publish your feedback."
					: "Your feedback is now online.";
		}
		return "You data has already been deleted. Please leave feedback before deleting you data.";
	}

	public Feedback one(final BigInteger id, final String pin) {
		return repository.one(Feedback.class, id);
	}

	@SuppressWarnings("unchecked")
	public List<Feedback> list() {
		return (List<Feedback>) repository.list(
				"select f.note, f.rating, f.answer, f.image, f.name from Feedback f where f.verified=true ORDER BY f.createdAt DESC");
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
