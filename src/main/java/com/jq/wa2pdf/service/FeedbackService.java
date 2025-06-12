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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jq.wa2pdf.entity.Feedback;

@Service
public class FeedbackService {
	@Autowired
	private Repository repository;

	public String save(final Feedback feedback) {
		repository.save(feedback);
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
}
