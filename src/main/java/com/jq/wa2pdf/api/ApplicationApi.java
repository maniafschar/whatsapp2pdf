package com.jq.wa2pdf.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jq.wa2pdf.service.ExtractService;
import com.jq.wa2pdf.service.PdfService;
import com.jq.wa2pdf.service.PdfService.Attributes;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("api")
public class ApplicationApi {
	@Autowired
	private PdfService pdfService;

	@Autowired
	private ExtractService extractService;

	@PostMapping("analyse")
	public Attributes analyse(@RequestParam("file") final MultipartFile file) throws Exception {
		final String id = "" + System.currentTimeMillis() + Math.random();
		extractService.unzip(file, id);
		return pdfService.analyse(id);
	}

	@PostMapping("conversion/{month}/{user}/{id}")
	public void conversion(@PathVariable String month, @PathVariable String user, @PathVariable String id)
			throws Exception {
		pdfService.create(id, month, user);
	}

	@GetMapping("pdf/{id}")
	public void pdf(@PathVariable final String id, final HttpServletResponse response)
			throws IOException, InterruptedException {
		final Path file = pdfService.get(id);
		if (file == null)
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "PDF not found for id: " + id);
		else {
			String name = pdfService.getFilename(id);
			if (name.contains("."))
				name = name.substring(0, name.lastIndexOf('.'));
			response.setHeader("Content-Disposition",
					"attachment; filename=\"" + name.replaceAll("[^a-zA-Z0-9.\\-_]", "") + ".pdf\"");
			IOUtils.copy(new FileInputStream(file.toAbsolutePath().toFile()), response.getOutputStream());
			response.flushBuffer();
		}
	}

	@DeleteMapping("cleanUp/{id}")
	public void cleanUp(@PathVariable final String id) throws IOException {
		extractService.cleanUp(id);
	}
}