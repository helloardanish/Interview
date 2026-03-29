package com.ar.Interview.controller;

import com.ar.Interview.service.ImageCompressionService;
import com.ar.Interview.service.PdfCompressionService;
import com.ar.Interview.util.FileSizeParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/files")
public class FileCompressionController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Value("${pdf.file-path}")
    private String pdfFilePath;

    @Value("${image.file-path}")
    private String imageFilePath;

    @Autowired
    private ImageCompressionService imageService;

    @Autowired
    private PdfCompressionService pdfService;

    // localhost:8080/api/v1/files/image/resize?path=/Users/helloardanish/Downloads/Poster.jpg&size=50kb
    @GetMapping("/image/resize")
    public ResponseEntity<String> resizeImage(
            @RequestParam String path,
            @RequestParam String size) {
        long targetSizeBytes = FileSizeParser.parse(size);

        Path outputPath = imageService.compressImage(path, targetSizeBytes);

        return ResponseEntity.ok("Compressed image saved at: " + outputPath);
    }

    // localhost:8080/api/v1/files/pdf/resize?path=/Users/helloardanish/Downloads/2026-syllabus.pdf&size=100kb
    @GetMapping("/pdf/resize")
    public ResponseEntity<String> resizePdf(
            @RequestParam String path,
            @RequestParam String size) {

        logger.info("Path is {} and size is {}", path, size);
        long targetSizeBytes = FileSizeParser.parse(size);

        // Path outputPath = pdfService.compressPdf(path, targetSizeBytes);
        Path outputPath = pdfService.forceCompressPdf(path, targetSizeBytes);

        return ResponseEntity.ok("Compressed PDF saved at: " + outputPath);
    }
}