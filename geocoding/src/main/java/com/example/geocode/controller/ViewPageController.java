package com.example.geocode.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ViewPageController {

    @RequestMapping({"/", "/up", "/upload"})
    public String upload() {
        return "upload";
    }

    @RequestMapping("/redirect-to-upload")
    public String redirectToUpload() {
        return "redirect:/";
    }

    @RequestMapping("/upload-page")
    public ModelAndView showUploadPage() {
        ModelAndView modelAndView = new ModelAndView("upload");
        modelAndView.addObject("message", "欢迎上传文件！");
        return modelAndView;
    }
}
