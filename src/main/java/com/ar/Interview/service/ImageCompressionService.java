package com.ar.Interview.service;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImageCompressionService {

    public Path compressImage(String inputPath, long targetSizeBytes) {

        Mat image = opencv_imgcodecs.imread(inputPath);

        String outputPath = generateOutputPath(inputPath);

        int quality = 90;

        while (quality > 10) {
            opencv_imgcodecs.imwrite(
                    outputPath,
                    image,
                    new int[]{opencv_imgcodecs.IMWRITE_JPEG_QUALITY, quality}
            );

            long fileSize = new java.io.File(outputPath).length();

            if (fileSize <= targetSizeBytes) {
                break;
            }

            quality -= 5;
        }

        return Paths.get(outputPath);
    }

    private String generateOutputPath(String inputPath) {
        return inputPath.replace(".", "_compressed.");
    }
}