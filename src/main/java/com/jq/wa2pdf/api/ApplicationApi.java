package com.jq.wa2pdf.api;

import java.awt.FontFormatException;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.mail.EmailException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;

import com.jq.wa2pdf.entity.Feedback;
import com.jq.wa2pdf.entity.Log;
import com.jq.wa2pdf.entity.Ticket;
import com.jq.wa2pdf.service.AdminService;
import com.jq.wa2pdf.service.ExtractService;
import com.jq.wa2pdf.service.ExtractService.Attributes;
import com.jq.wa2pdf.service.FeedbackService;
import com.jq.wa2pdf.service.PdfService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("api")
public class ApplicationApi {
	@Autowired
	private PdfService pdfService;

	@Autowired
	private ExtractService extractService;

	@Autowired
	private FeedbackService feedbackService;

	@Autowired
	private AdminService adminService;

	@PostMapping("pdf/analyse")
	public Attributes analyse(@RequestParam("file") final MultipartFile file) {
		try {
			return this.extractService.unzip(file, "" + System.currentTimeMillis() + Math.random());
		} catch(IOException e) {
			return null;
		}
	}

	@PostMapping("pdf/preview/{id}")
	public void preview(@PathVariable final String id, @RequestParam final String period,
			@RequestParam final String user) throws IOException, FontFormatException, ParseException {
		this.pdfService.create(id, period, user, true);
	}

	@PostMapping("pdf/buy/{id}")
	public void buy(@PathVariable final String id, @RequestParam final String[] periods,
			@RequestParam final String user) throws IOException, FontFormatException, ParseException {
		for (final String period : periods)
			this.pdfService.create(id, period, user, false);
	}

	@GetMapping("pdf/{id}")
	public void pdf(@PathVariable final String id, @RequestParam(required = false) final String period,
			final HttpServletResponse response) throws IOException {
		final Path file = this.pdfService.get(id, period);
		if (file == null) {
			if (!Files.exists(ExtractService.getTempDir(id)))
				throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid ID");
			final Path path = ExtractService.getTempDir(id)
					.resolve(PdfService.filename + "Error" + (period == null ? "" : period));
			if (Files.exists(path))
				throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
						IOUtils.toString(path.toUri().toURL(), StandardCharsets.UTF_8));
			response.sendError(Log.STATUS_PROCESSING_PDF);
		} else {
			response.setHeader("Content-Disposition",
					"attachment; filename=\"" + this.sanatizeFilename(this.extractService.getFilename(id)) +
							this.sanatizePeriod(period) + ".pdf\"");
			IOUtils.copy(new FileInputStream(file.toAbsolutePath().toFile()), response.getOutputStream());
			response.flushBuffer();
		}
	}

	@DeleteMapping("pdf/{id}")
	public void delete(@PathVariable final String id) throws IOException {
		this.extractService.delete(id);
	}

	@GetMapping("feedback/{id}/{pin}")
	public Feedback feedback(@PathVariable final BigInteger id, @PathVariable final String pin) {
		return this.feedbackService.one(id, pin);
	}

	@PutMapping("feedback/confirm")
	public String feedbackConfim(@RequestBody final Feedback feedback) throws EmailException {
		return this.feedbackService.confirm(feedback);
	}

	@PutMapping("feedback/{id}")
	public String feedbackSave(@PathVariable final String id, @RequestBody final Feedback feedback)
			throws EmailException {
		return this.feedbackService.save(id, feedback);
	}

	@GetMapping("feedback/list")
	public List<Feedback> feedbackList() {
		return this.feedbackService.list();
	}

	@PostMapping("ticket")
	public void ticket(@RequestBody final Ticket ticket) {
		this.adminService.createTicket(ticket);
	}

	private String sanatizeFilename(String filename) {
		if (filename.contains("."))
			filename = filename.substring(0, filename.lastIndexOf('.'));
		return filename.replaceAll("[^a-zA-Z0-9.\\-_]", "");
	}

	private String sanatizePeriod(String period) {
		if (period == null)
			return "";
		if (period.contains("/"))
			period = period.replace("/\\d\\d", "").replace("/", "_");
		else if (period.contains("."))
			period = period.replace("\\d\\d.", "").replace(".", "_");
		else if (period.contains("-"))
			period = period.substring(0, period.lastIndexOf('-')).replace("-", "_");
		return "_" + period;
	}
}
