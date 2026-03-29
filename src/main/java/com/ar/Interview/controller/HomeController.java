package com.ar.Interview.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @GetMapping("/test")
    public String home(Model model) {
        logger.info("/test called");
        model.addAttribute("name", "A R");
        return "test"; // refers to resources/templates/test.html
    }
}
