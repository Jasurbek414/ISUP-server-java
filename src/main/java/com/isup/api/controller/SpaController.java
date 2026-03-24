package com.isup.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA fallback controller — serves React index.html for all non-API routes.
 * Enables client-side routing (React Router) to work correctly.
 * Spring MVC resolves more-specific patterns first, so /api/**, /health,
 * /metrics, /ws handlers in other controllers take precedence over this.
 */
@Controller
public class SpaController {

    /** Catch-all: any path segment that contains no dot (not a static file) */
    @RequestMapping({
            "/",
            "/{path:[^.]*}",
            "/{path:[^.]*}/{sub:[^.]*}",
            "/{path:[^.]*}/{sub:[^.]*}/{third:[^.]*}"
    })
    public String spa() {
        return "forward:/index.html";
    }
}
