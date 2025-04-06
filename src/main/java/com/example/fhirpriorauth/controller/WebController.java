package com.example.fhirpriorauth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for web pages
 */
@Controller
public class WebController {

    /**
     * Redirect root to the home page
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/index.html";
    }

    /**
     * Redirect /auth to the auth test page
     */
    @GetMapping("/auth")
    public String auth() {
        return "redirect:/auth.html";
    }

    /**
     * Redirect /submit to the submit test page
     */
    @GetMapping("/submit")
    public String submit() {
        return "redirect:/submit.html";
    }
}
