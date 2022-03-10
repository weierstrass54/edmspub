package com.ckontur.edms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@Controller
public class SwaggerController {

    @GetMapping("/")
    public ModelAndView root() {
        return new ModelAndView("redirect:/swagger-ui/");
    }

}
