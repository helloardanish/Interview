package com.ar.Interview.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/test")
    public String home(Model model) {
        model.addAttribute("name", "A R");
        return "test"; // refers to index.html
    }
}
