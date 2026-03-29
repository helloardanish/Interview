package com.ar.Interview.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import net.sourceforge.tess4j.Tesseract;

import org.bytedeco.opencv.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

import com.madgag.gif.fmsware.AnimatedGifEncoder;

@Service
public class ImageService {

    private final Tesseract tesseract;

    public ImageService() {
        tesseract = new Tesseract();
        tesseract.setDatapath("tessdata");
        tesseract.setTessVariable("tessedit_char_whitelist", "0123456789");
    }

    static {
        getVersionString();
    }

    public String processImage(MultipartFile file) throws Exception {

        Files.createDirectories(Paths.get("output"));

        String inputPath = "input.png";
        Files.write(Paths.get(inputPath), file.getBytes());

        Mat image = imread(inputPath);
        if (image.empty())
            throw new RuntimeException("Image not loaded");

        Mat gray = new Mat();
        cvtColor(image, gray, COLOR_BGR2GRAY);

        GaussianBlur(gray, gray, new Size(5, 5), 0);

        Mat thresh = new Mat();
        threshold(gray, thresh, 150, 255, THRESH_BINARY_INV);

        // 🔹 Detect grid
        MatVector contours = new MatVector();
        findContours(thresh, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

        List<Rect> boxes = new ArrayList<>();

        for (int i = 0; i < contours.size(); i++) {
            Rect rect = boundingRect(contours.get(i));

            if (rect.width() > 30 && rect.height() > 30) {
                boxes.add(rect);
            }
        }

        // 🔹 Sort boxes
        boxes.sort((a, b) -> {
            if (Math.abs(a.y() - b.y()) > 20)
                return Integer.compare(a.y(), b.y());
            return Integer.compare(a.x(), b.x());
        });

        int gridSize = (int) Math.sqrt(boxes.size());
        int[][] grid = new int[gridSize][gridSize];

        // 🔥 OCR
        int index = 0;
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {

                Rect r = boxes.get(index++);
                Mat roi = new Mat(gray, r);

                int number = detectNumber(roi);
                grid[i][j] = number;
            }
        }

        // 🔹 Find start
        int startX = -1, startY = -1, max = 0;

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {

                if (grid[i][j] == 1) {
                    startX = i;
                    startY = j;
                }

                max = Math.max(max, grid[i][j]);
            }
        }

        if (startX == -1)
            throw new RuntimeException("1 not found");

        // 🔹 Solve
        solve(grid, startX, startY, 1, gridSize * gridSize);

        // 🔹 Path map
        Map<Integer, Point> pathMap = buildPathMap(grid, boxes, gridSize);

        // 🔹 GIF
        generateGif(image.clone(), pathMap, "output/solution.gif");

        // 🔹 Final image
        drawSolution(image, pathMap);
        imwrite("output/result.png", image);

        return "solution.gif";
    }

    // 🔥 OCR
    private int detectNumber(Mat roi) {

        try {
            Mat processed = new Mat();
            threshold(roi, processed, 120, 255, THRESH_BINARY);

            java.awt.image.BufferedImage img = matToBufferedImage(processed);

            String result = tesseract.doOCR(img).trim();

            if (result.matches("\\d+")) {
                return Integer.parseInt(result);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // 🔥 SOLVER
    private boolean solve(int[][] grid, int x, int y, int current, int max) {

        if (current == max)
            return true;

        int[] dx = { 0, 1, 0, -1 };
        int[] dy = { 1, 0, -1, 0 };

        for (int d = 0; d < 4; d++) {

            int nx = x + dx[d];
            int ny = y + dy[d];

            if (nx < 0 || ny < 0 || nx >= grid.length || ny >= grid[0].length)
                continue;

            if (grid[nx][ny] == current + 1 || grid[nx][ny] == 0) {

                int temp = grid[nx][ny];

                if (grid[nx][ny] == 0)
                    grid[nx][ny] = current + 1;

                if (solve(grid, nx, ny, current + 1, max))
                    return true;

                if (temp == 0)
                    grid[nx][ny] = 0;
            }
        }

        return false;
    }

    // 🔹 Path map
    private Map<Integer, Point> buildPathMap(int[][] grid, List<Rect> boxes, int size) {

        Map<Integer, Point> map = new HashMap<>();

        int index = 0;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {

                Rect r = boxes.get(index++);

                int cx = r.x() + r.width() / 2;
                int cy = r.y() + r.height() / 2;

                map.put(grid[i][j], new Point(cx, cy));
            }
        }

        return map;
    }

    // 🎬 GIF
    private void generateGif(Mat original, Map<Integer, Point> pathMap, String path) {

        AnimatedGifEncoder gif = new AnimatedGifEncoder();
        gif.start(path);
        gif.setDelay(400);
        gif.setRepeat(0);

        Mat frame = original.clone();

        List<Integer> keys = new ArrayList<>(pathMap.keySet());
        Collections.sort(keys);

        for (int i = 0; i < keys.size() - 1; i++) {

            line(
                    frame,
                    pathMap.get(keys.get(i)),
                    pathMap.get(keys.get(i + 1)),
                    new Scalar(0, 0, 255, 0),
                    2,
                    LINE_AA,
                    0);

            gif.addFrame(matToBufferedImage(frame));
        }

        gif.finish();
    }

    private void drawSolution(Mat image, Map<Integer, Point> map) {

        List<Integer> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);

        for (int i = 0; i < keys.size() - 1; i++) {

            line(
                    image,
                    map.get(keys.get(i)),
                    map.get(keys.get(i + 1)),
                    new Scalar(0, 0, 255, 0),
                    2,
                    LINE_AA,
                    0);
        }
    }

    private java.awt.image.BufferedImage matToBufferedImage(Mat mat) {

        int type = java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] b = new byte[bufferSize];
        mat.data().get(b);

        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(mat.cols(), mat.rows(), type);

        byte[] targetPixels = ((java.awt.image.DataBufferByte) img.getRaster().getDataBuffer()).getData();

        System.arraycopy(b, 0, targetPixels, 0, b.length);

        return img;
    }
}