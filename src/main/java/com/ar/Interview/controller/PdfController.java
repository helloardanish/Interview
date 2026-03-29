package com.ar.Interview.controller;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.ByteArrayOutputStream;
import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/pdf")
public class PdfController {

    @Value("${pdf.file-path}")
    private String filePath;

    // @GetMapping("/no-of-pages/{from}/{to}")
    // public String noOfPages(@PathVariable int from,
    // @PathVariable int to) {

    // /no-of-pages?from=1&to=262
    // http://localhost:8080/no-of-pages?from=263&to=299
    @GetMapping("/no-of-pages")
    public ResponseEntity<byte[]> noOfPages(@RequestParam int from,
            @RequestParam int to) throws IOException {

        File file = new File(filePath);

        try (PDDocument originalDoc = Loader.loadPDF(file);
                PDDocument newDoc = new PDDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            int totalPages = originalDoc.getNumberOfPages();

            if (from < 1 || to > totalPages || from > to) {
                throw new IllegalArgumentException("Invalid page range");
            }

            for (int i = from - 1; i < to; i++) {
                PDPage page = originalDoc.getPage(i);
                newDoc.addPage(page);
            }

            newDoc.save(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=output.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(out.toByteArray());
        }
    }
}
