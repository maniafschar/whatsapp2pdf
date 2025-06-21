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

	public String confirm(final Feedback feedback) throws EmailException {
		final Feedback original = this.repository.one(Feedback.class, feedback.getId());
		if (!original.getPin().equals(feedback.getPin()))
			return "Pin expired.";
		original.setVerified(true);
		original.setPin(this.generatePin(6));
		this.repository.save(original);
		this.emailService.send(original.getEmail(), "?id=" + original.getId() + "&pin=" + original.getPin());
		return "Your feedback is now online.";
	}

	public String save(final String id, Feedback feedback) throws EmailException {
		if (Strings.isEmpty(feedback.getName()) || Strings.isEmpty(feedback.getEmail())
				|| Strings.isEmpty(feedback.getNote()))
			return "No input.";
		if (Files.exists(ExtractService.getTempDir(id))) {
			final boolean isNew = feedback.getId() == null;
			if (!isNew) {
				final Feedback original = this.repository.one(Feedback.class, feedback.getId());
				if (!original.getPin().equals(feedback.getPin()))
					return "Pin expired.";
				if (!Strings.isEmpty(feedback.getNote()))
					original.setNote(feedback.getNote());
				feedback = original;
				feedback.setVerified(true);
			}
			feedback.setPin(this.generatePin(6));
			this.repository.save(feedback);
			this.emailService.send(feedback.getEmail(), "?id=" + feedback.getId() + "&pin=" + feedback.getPin());
			return isNew ? "An email has been sent to you. Please confirm to publish your feedback."
					: "Your feedback is now online.";
		}
		return "You data has already been deleted. Please leave feedback before deleting you data.";
	}

	public Feedback one(final BigInteger id, final String pin) {
		return this.repository.one(Feedback.class, id);
	}

	@SuppressWarnings("unchecked")
	public List<Feedback> list() {
		return (List<Feedback>) this.repository.list(
				"select feedback.note, feedback.rating, feedback.answer, feedback.image, feedback.name from Feedback feedback where verified=true ORDER BY createdAt DESC");
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
