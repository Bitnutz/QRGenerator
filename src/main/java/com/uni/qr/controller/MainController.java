package com.uni.qr.controller;

import com.amazonaws.util.IOUtils;
import com.uni.qr.model.Log;
import com.uni.qr.model.URL;
import com.uni.qr.repository.LogRepository;
import com.uni.qr.repository.UrlRepository;
import com.uni.qr.service.GenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Timestamp;

@Controller
public class MainController {

    @Autowired
    private UrlRepository urlRepository;
    @Autowired
    private GenerationService service;
    @Autowired
    private LogRepository logRepository;

    @PostMapping(path="/add") // Map ONLY POST Requests
    @Transactional
    public @ResponseBody
    String addNewUrl (@RequestParam String vanillaUrl,
                      @RequestParam String description,
                      @RequestParam Integer width,
                      @RequestParam Integer height) {

        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());

        URL urlObject = new URL();
        urlObject.setVanilla_URL(vanillaUrl);
        urlObject.setDescription(description);
        urlObject.setDeleteFlag(false);
        urlObject.setTimestamp(currentTimestamp);
        urlObject.setResult(service.convertByteArrayToBlob(GenerationService.getQRCodeImage(vanillaUrl, width,height)));

        urlRepository.save(urlObject);


        Log currentLog = new Log();
        currentLog.setAction("inserted a row into URL table!");
        currentLog.setTimestamp(currentTimestamp);

        logRepository.save(currentLog);
        return "Saved!"+ "\nUID = " + urlObject.getUid();
    }


    @GetMapping("/get")
    @Transactional
    public void getQR(HttpServletResponse response, @RequestParam Integer uid) throws IOException, SQLException {
        response.setContentType("image/jpeg");

        Log currentLog = new Log();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        if(!urlRepository.findById(uid).isPresent()) {
            currentLog.setAction("Tried to access a row, which does not exists");
            currentLog.setTimestamp(timestamp);
            return;
        }
        if(urlRepository.findById(uid).get().isDeleteFlag()) {
            currentLog.setAction("Tried to access a row, which was deleted");
            currentLog.setTimestamp(timestamp);
            return;
        }
        currentLog.setAction(String.format("Fetched the QR code with uid: %d", uid));
        currentLog.setTimestamp(timestamp);
        logRepository.save(currentLog);
        URL currentUrl = urlRepository.findById(uid).get();

        byte[] imageBytes = com.amazonaws.util.IOUtils.toByteArray(currentUrl.getResult().getBinaryStream());
        InputStream in = new ByteArrayInputStream(imageBytes);
        IOUtils.copy(in, response.getOutputStream());
    }


    @PostMapping("/delete")
    @Transactional
    public @ResponseBody String deleteQR(@RequestParam Integer uid) {
        Log currentLog = new Log();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());


        if(!urlRepository.findById(uid).isPresent()) {
            currentLog.setAction("Tried to delete a row, which not exists");
            currentLog.setTimestamp(timestamp);
            logRepository.save(currentLog);
            return null;
        }
        if(urlRepository.findById(uid).get().isDeleteFlag()) {
            currentLog.setAction("Tried to delete a row, which is already deleted");
            currentLog.setTimestamp(timestamp);
            logRepository.save(currentLog);
            return null;
        }
        URL currentUrl = urlRepository.findById(uid).get();
        currentUrl.setDeleteFlag(true);
        urlRepository.save(currentUrl);

        return "Deleted!";
    }
}
