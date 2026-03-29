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

        int low = 10;
        int high = 100;

        int bestQuality = high;
        long closestSizeDiff = Long.MAX_VALUE;

        while (low <= high) {

            int mid = (low + high) / 2;

            opencv_imgcodecs.imwrite(
                    outputPath,
                    image,
                    new int[] { opencv_imgcodecs.IMWRITE_JPEG_QUALITY, mid });

            long fileSize = new java.io.File(outputPath).length();
            long diff = Math.abs(fileSize - targetSizeBytes);

            // Track closest result
            if (diff < closestSizeDiff) {
                closestSizeDiff = diff;
                bestQuality = mid;
            }

            if (fileSize > targetSizeBytes) {
                high = mid - 1; // reduce quality
            } else {
                low = mid + 1; // increase quality
            }
        }

        // Final write with best quality
        opencv_imgcodecs.imwrite(
                outputPath,
                image,
                new int[] { opencv_imgcodecs.IMWRITE_JPEG_QUALITY, bestQuality });

        return Paths.get(outputPath);
    }

    private String generateOutputPath(String inputPath) {
        return inputPath.replace(".", "_compressed.");
    }
}