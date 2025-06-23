package com.jq.wa2pdf.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import com.jq.wa2pdf.service.ExtractService;
import com.jq.wa2pdf.service.ExtractService.Attributes;
import com.jq.wa2pdf.service.FeedbackService;
import com.jq.wa2pdf.service.PdfService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("sc")
public class ApplicationApi {
	@Autowired
	private AdminService adminService;

	@GetMapping("init")
	public AdminData init() throws Exception {
		return this.adminService.init();
	}
}
