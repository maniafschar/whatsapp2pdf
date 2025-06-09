package com.jq.wa2pdf.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;

import com.jq.wa2pdf.service.ExtractService;
import com.jq.wa2pdf.service.ExtractService.Attributes;
import com.jq.wa2pdf.service.PdfService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("api")
public class ApplicationApi {
	@Autowired
	private PdfService pdfService;

	@Autowired
	private ExtractService extractService;

	@PostMapping("analyse")
	public Attributes analyse(@RequestParam("file") final MultipartFile file)
			throws Exception {
		final String id = "" + System.currentTimeMillis() + Math.random();
		return extractService.unzip(file, id);
	}

	@PostMapping("preview/{id}")
	public void preview(@PathVariable String id, @RequestParam String period, @RequestParam String user)
			throws Exception {
		pdfService.create(id, period, user, false);
	}

	@GetMapping("pdf/{id}")
	public void pdf(@PathVariable final String id, final HttpServletResponse response)
			throws IOException, InterruptedException {
		final Path file = pdfService.get(id);
		if (file == null)
			throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
					Files.exists(ExtractService.getTempDir(id)) ? "PDF not created" : "Invalid ID");
		response.setHeader("Content-Disposition",
				"attachment; filename=\"" + sanatizeFilename(extractService.getFilename(id)) +
						sanatizePeriod(pdfService.getPeriod(id)) + ".pdf\"");
		IOUtils.copy(new FileInputStream(file.toAbsolutePath().toFile()), response.getOutputStream());
		response.flushBuffer();
	}

	private String sanatizeFilename(String filename) {
		if (filename.contains("."))
			filename = filename.substring(0, filename.lastIndexOf('.'));
		return filename.replaceAll("[^a-zA-Z0-9.\\-_]", "");
	}

	private String sanatizePeriod(String period) {
		if (period.contains("/"))
			period = period.replace("/\\d\\d", "");
		else if (period.contains("."))
			period = period.replace("\\d\\d.", "");
		else if (period.contains("-"))
			period = period.substring(0, period.lastIndexOf('-'));
		return "_" + period;
	}

	@DeleteMapping("cleanUp/{id}")
	public void cleanUp(@PathVariable final String id) throws IOException {
		extractService.cleanUp(id);
	}
}