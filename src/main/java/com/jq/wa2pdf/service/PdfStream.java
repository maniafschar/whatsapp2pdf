package com.jq.wa2pdf.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfStream {
	@Autowired
	private ExtractService extractService;

	@Autowired
	private PdfService pdfService;

	@Transactional(readOnly = true)
	public String conversion(final MultipartFile file) {
		try {
			final String id = "" + System.currentTimeMillis() + Math.random();
			pdfService.create(extractService.unzip(file, id));
			return id;
		} catch (Exception e) {
			throw new RuntimeException("Error converting file", e);
		}
	}
}