package com.maintenance.maintenance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String redirectToLogin() {
        return "redirect:/login";
    }

    // Rediriger login.html vers /login (pour éviter les erreurs 404)
    @GetMapping("/login.html")
    public String redirectLoginHtml() {
        return "redirect:/login";
    }

    // Rediriger index.html vers /dashboard
    @GetMapping("/index.html")
    public String redirectIndexHtml() {
        return "redirect:/dashboard";
    }

    @GetMapping("/alerts")
    public String alerts() {
        return "index";
    }

    @GetMapping("/repairs")
    public String repairs() {
        return "index";
    }

    @GetMapping("/reports")
    public String reports() {
        // Le RapportsController gère maintenant cette route
        return "redirect:/rapports";
    }

    @GetMapping("/calendar")
    public String calendar() {
        return "index";
    }

    @GetMapping("/inventory")
    public String inventory() {
        return "index";
    }

}



