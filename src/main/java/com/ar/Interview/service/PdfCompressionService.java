package com.ar.Interview.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

@Service
public class PdfCompressionService {

    public Path compressPdf(String inputPath, long targetSizeBytes) {

        String outputPath = inputPath.replace(".pdf", "_compressed.pdf");

        int low = 10;
        int high = 100;
        int bestQuality = high;
        long closestDiff = Long.MAX_VALUE;

        while (low <= high) {

            int mid = (low + high) / 2;

            File tempFile = new File("temp_" + mid + ".pdf");

            try (PDDocument document = Loader.loadPDF(new File(inputPath))) {

                for (PDPage page : document.getPages()) {
                    PDResources resources = page.getResources();

                    for (var name : resources.getXObjectNames()) {

                        if (resources.isImageXObject(name)) {

                            PDImageXObject image = (PDImageXObject) resources.getXObject(name);

                            BufferedImage bufferedImage = image.getImage();

                            byte[] compressedBytes = compressBufferedImage(bufferedImage, mid);

                            PDImageXObject compressedImage = PDImageXObject.createFromByteArray(
                                    document,
                                    compressedBytes,
                                    "compressed");

                            resources.put(name, compressedImage);
                        }
                    }
                }

                document.save(tempFile);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            long fileSize = tempFile.length();
            long diff = Math.abs(fileSize - targetSizeBytes);

            if (diff < closestDiff) {
                closestDiff = diff;
                bestQuality = mid;
            }

            if (fileSize > targetSizeBytes) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }

            tempFile.delete();
        }

        // Final pass with best quality
        applyFinalCompression(inputPath, outputPath, bestQuality);

        return Paths.get(outputPath);
    }

    private void applyFinalCompression(String inputPath, String outputPath, int quality) {

        try (PDDocument document = Loader.loadPDF(new File(inputPath))) {

            for (PDPage page : document.getPages()) {
                PDResources resources = page.getResources();

                for (var name : resources.getXObjectNames()) {

                    if (resources.isImageXObject(name)) {

                        PDImageXObject image = (PDImageXObject) resources.getXObject(name);

                        BufferedImage bufferedImage = image.getImage();

                        byte[] compressedBytes = compressBufferedImage(bufferedImage, quality);

                        PDImageXObject compressedImage = PDImageXObject.createFromByteArray(
                                document,
                                compressedBytes,
                                "compressed");

                        resources.put(name, compressedImage);
                    }
                }
            }

            document.save(outputPath);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] compressBufferedImage(BufferedImage image, int quality)
            throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");

        ImageWriter writer = writers.next();

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality / 100f);

        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);

        writer.write(null, new IIOImage(image, null, null), param);

        writer.dispose();
        ios.close();

        return baos.toByteArray();
    }
}