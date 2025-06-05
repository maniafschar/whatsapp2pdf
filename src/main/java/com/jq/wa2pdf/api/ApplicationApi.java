package com.jq.wa2pdf.api;

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jq.wa2pdf.service.PdfService;
import com.jq.wa2pdf.service.PdfStream;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("api")
public class ApplicationApi {
	@Autowired
	private PdfStream pdfStream;

	@Autowired
	private PdfService pdfService;

	@PostMapping("conversion")
	public String conversion(@RequestParam("file") final MultipartFile file) throws Exception {
		return pdfStream.conversion(file);
	}

	@GetMapping("pdf/{id}/{name}")
	public void pdf(@PathVariable final String id, @PathVariable final String name, final HttpServletResponse response)
			throws IOException {
		response.setHeader("Content-Disposition", "attachment; filename=\"" + name + ".pdf\"");
		IOUtils.copy(new FileInputStream(pdfService.getPdf(id).toAbsolutePath().toFile()), response.getOutputStream());
		response.flushBuffer();
	}
}