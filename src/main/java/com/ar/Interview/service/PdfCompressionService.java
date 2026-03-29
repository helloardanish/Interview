package com.ar.Interview.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

@Service
public class PdfCompressionService {

    private static final long TOLERANCE = 5 * 1024; // ±5KB
    private static final int MIN_QUALITY = 10;
    private static final int MAX_QUALITY = 100;

    private static final long TARGET_TOLERANCE = 5 * 1024;

    public Path compressPdf(String inputPath, long targetSizeBytes) {

        String outputPath = inputPath.replace(".pdf", "_compressed.pdf");

        int low = MIN_QUALITY;
        int high = MAX_QUALITY;

        byte[] bestResult = null;
        long closestDiff = Long.MAX_VALUE;

        while (low <= high) {

            int mid = (low + high) / 2;

            byte[] compressedPdf = compressToBytes(inputPath, mid);
            long size = compressedPdf.length;

            long diff = Math.abs(size - targetSizeBytes);

            // Save best match
            if (diff < closestDiff) {
                closestDiff = diff;
                bestResult = compressedPdf;
            }

            // 🎯 Early exit if within tolerance
            if (diff <= TOLERANCE) {
                bestResult = compressedPdf;
                break;
            }

            if (size > targetSizeBytes) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        // Final write
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(bestResult);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write compressed PDF", e);
        }

        return Paths.get(outputPath);
    }

    private byte[] compressToBytes(String inputPath, int quality) {

        try (PDDocument document = Loader.loadPDF(new File(inputPath));
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            for (PDPage page : document.getPages()) {

                PDResources resources = page.getResources();

                for (var name : resources.getXObjectNames()) {

                    if (!resources.isImageXObject(name))
                        continue;

                    try {
                        PDImageXObject image = (PDImageXObject) resources.getXObject(name);

                        BufferedImage bufferedImage = image.getImage();

                        // Skip tiny images (performance optimization)
                        if (bufferedImage.getWidth() < 100 ||
                                bufferedImage.getHeight() < 100) {
                            continue;
                        }

                        byte[] compressedImage = compressImage(bufferedImage, quality);

                        PDImageXObject newImage = PDImageXObject.createFromByteArray(
                                document,
                                compressedImage,
                                "compressed");

                        resources.put(name, newImage);

                    } catch (Exception e) {
                        // Skip problematic images (JBIG2, CCITT, etc.)
                        continue;
                    }
                }
            }

            document.save(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Compression failed", e);
        }
    }

    private byte[] compressImage(BufferedImage image, int quality)
            throws IOException {

        // ✅ Convert to RGB (fix for "Bogus input colorspace")
        BufferedImage rgbImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g = rgbImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");

        if (!writers.hasNext()) {
            throw new RuntimeException("No JPEG writer available");
        }

        ImageWriter writer = writers.next();

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality / 100f);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {

            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgbImage, null, null), param);
        }

        writer.dispose();

        return baos.toByteArray();
    }

    public Path forceCompressPdf(String inputPath, long targetSizeBytes) {

        String outputPath = inputPath.replace(".pdf", "_forced.pdf");

        int low = 10;
        int high = 100;

        byte[] bestResult = null;
        long closestDiff = Long.MAX_VALUE;

        while (low <= high) {

            int quality = (low + high) / 2;

            byte[] pdfBytes = rebuildPdfAsImages(inputPath, quality);
            long size = pdfBytes.length;

            long diff = Math.abs(size - targetSizeBytes);

            if (diff < closestDiff) {
                closestDiff = diff;
                bestResult = pdfBytes;
            }

            if (diff <= TARGET_TOLERANCE)
                break;

            if (size > targetSizeBytes) {
                high = quality - 1;
            } else {
                low = quality + 1;
            }
        }

        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(bestResult);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Paths.get(outputPath);
    }

    private byte[] rebuildPdfAsImages(String inputPath, int quality) {

        try (PDDocument original = Loader.loadPDF(new File(inputPath));
                PDDocument newDoc = new PDDocument();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDFRenderer renderer = new PDFRenderer(original);

            for (int i = 0; i < original.getNumberOfPages(); i++) {

                // 🎯 Render page as image (lower DPI for more compression)
                BufferedImage image = renderer.renderImageWithDPI(i, 100);

                // 🔥 Resize dynamically
                int newWidth = (int) (image.getWidth() * (quality / 100.0));
                int newHeight = (int) (image.getHeight() * (quality / 100.0));

                BufferedImage scaled = new BufferedImage(
                        newWidth,
                        newHeight,
                        BufferedImage.TYPE_INT_RGB);

                Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(image, 0, 0, newWidth, newHeight, null);
                g.dispose();

                // Compress image
                byte[] compressedImage = compressJpeg(scaled, quality);

                PDImageXObject pdImage = PDImageXObject.createFromByteArray(
                        newDoc,
                        compressedImage,
                        "page-" + i);

                PDPage page = new PDPage(
                        new PDRectangle(pdImage.getWidth(), pdImage.getHeight()));

                newDoc.addPage(page);

                try (PDPageContentStream content = new PDPageContentStream(newDoc, page)) {

                    content.drawImage(pdImage, 0, 0);
                }
            }

            newDoc.save(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] compressJpeg(BufferedImage image, int quality)
            throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");

        ImageWriter writer = writers.next();

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality / 100f);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {

            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        }

        writer.dispose();

        return baos.toByteArray();
    }
}