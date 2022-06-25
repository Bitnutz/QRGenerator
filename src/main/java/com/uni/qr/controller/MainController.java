package com.uni.qr.controller;

import com.amazonaws.util.IOUtils;
import com.uni.qr.model.URL;
import com.uni.qr.service.GenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

@Controller
public class MainController {

    @Autowired
    private GenerationService service;

    @PostMapping(path="/add")
    public @ResponseBody
    String addNewUrl (@RequestParam String vanillaUrl,
                      @RequestParam String description,
                      @RequestParam Integer width,
                      @RequestParam Integer height
    ) {
        URL urlObject = service.addURL(vanillaUrl, description, width, height);
        return String.format("Saved!\nUID = %d%n", urlObject.getUid());
    }

    @GetMapping("/get")
    public String getQR(HttpServletResponse response, @RequestParam Integer uid, Model model) throws IOException, SQLException {
        response.setContentType("image/png");
        InputStream in = service.getQrFinal(uid);
        if(in == null) {
            model.addAttribute("Unable to fetch deleted or non-existing QR code!", "Get mapping failed");
            return "error";
        }
        IOUtils.copy(in, response.getOutputStream());
        response.getOutputStream().flush();
        return "QR returned";
    }


    @PostMapping("/delete")
    public @ResponseBody String deleteQR(@RequestParam Integer uid) {
        return service.deleteGeneratedQr(uid);
    }
}
