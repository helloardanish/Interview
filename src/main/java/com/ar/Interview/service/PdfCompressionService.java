package com.ar.Interview.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class PdfCompressionService {

    public Path compressPdf(String inputPath, long targetSizeBytes) {

        String outputPath = inputPath.replace(".pdf", "_compressed.pdf");

        try (PDDocument document = Loader.loadPDF(new File(inputPath))) {

            document.getPages().forEach(page -> {
                page.getResources().getXObjectNames().forEach(name -> {
                    try {
                        if (page.getResources().isImageXObject(name)) {
                            PDImageXObject image = (PDImageXObject) page.getResources().getXObject(name);

                            // Attempt compression (basic)
                            image.getCOSObject().setItem(
                                    org.apache.pdfbox.cos.COSName.FILTER,
                                    org.apache.pdfbox.cos.COSName.DCT_DECODE);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            });

            document.save(outputPath);

        } catch (IOException e) {
            throw new RuntimeException("PDF compression failed", e);
        }

        return Paths.get(outputPath);
    }
}