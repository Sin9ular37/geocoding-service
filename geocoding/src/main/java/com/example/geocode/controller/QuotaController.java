package com.example.geocode.controller;

import com.example.geocode.service.impl.QuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v2/api")
public class QuotaController {
    
    private final QuotaService quotaService;
    
    @Autowired
    public QuotaController(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @GetMapping("/quota-gaode-monthly")
    public int getCurrentValue() {
        return quotaService.getCurrentValue();
    }

    @GetMapping("/quota-tianditu-daily")
    public int getCurrentValueCopy() {
        return quotaService.getCurrentValueDaily();
    }

}