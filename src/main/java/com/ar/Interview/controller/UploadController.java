package com.ar.Interview.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ar.Interview.service.ImageService;

@Controller
public class UploadController {
    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

    private final ImageService imageService;

    public UploadController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/")
    public String index() {
        logger.info("Returning index page");
        return "index";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, Model model) throws Exception {

        String resultFile = imageService.processImage(file);

        model.addAttribute("resultImage", resultFile);
        return "index";
    }

    @GetMapping("/result/{name}")
    @ResponseBody
    public byte[] getImage(@PathVariable String name) throws IOException {
        return Files.readAllBytes(Paths.get("output/" + name));
    }
}
