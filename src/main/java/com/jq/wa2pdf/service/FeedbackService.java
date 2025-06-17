package com.jq.wa2pdf.service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
		if (Files.exists(ExtractService.getTempDir(id))) {
			repository.save(feedback);
			emailService.send(feedback.getEmail(), "ydfgdf");
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
}
