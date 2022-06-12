package com.uni.qr.service;

import com.amazonaws.util.IOUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.uni.qr.model.Log;
import com.uni.qr.model.URL;
import com.uni.qr.repository.LogRepository;
import com.uni.qr.repository.UrlRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.sql.rowset.serial.SerialBlob;
import javax.transaction.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Timestamp;

@Service
public class GenerationService {

    private static final Logger logger = LogManager.getLogger(StorageService.class);

    @Autowired
    UrlRepository urlRepository;

    @Autowired
    LogRepository logRepository;

    @Transactional
    public URL addURL(String vanillaUrl, String description, Integer width, Integer height) {
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());

        URL urlObject = new URL();
        urlObject.setVanilla_URL(vanillaUrl);
        urlObject.setDescription(description);
        urlObject.setDeleteFlag(false);
        urlObject.setTimestamp(currentTimestamp);
        urlObject.setResult(convertByteArrayToBlob(GenerationService.getQRCodeImage(vanillaUrl, width,height)));

        urlRepository.save(urlObject);

        Log currentLog = new Log();
        currentLog.setAction("inserted a row into URL table!");
        currentLog.setTimestamp(currentTimestamp);

        logRepository.save(currentLog);
        return urlObject;
    }


    public static byte[] getQRCodeImage(String text, int width, int height) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix;
        try {
            bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            logger.info("Processed the URL successfully");
            return pngOutputStream.toByteArray();
        } catch (WriterException | IOException e) {
            logger.error("Couldn't process the URL for creating an image with the following error: ", e);
        }
        return null;
    }

    public byte[] getImage(Integer uid) throws IOException, SQLException {
        if(!urlRepository.findById(uid).isPresent() && urlRepository.findById(uid).get().isDeleteFlag()) {
            logger.info(String.format("The searched object is either deleted or is missing: %d", uid));
            return null;
        }
        logger.info(String.format("The searched object is found: %d", uid));
        URL currentUrl = urlRepository.findById(uid).get();

        return IOUtils.toByteArray(currentUrl.getResult().getBinaryStream());
    }

    public Blob convertByteArrayToBlob(byte[] image)  {
        Blob result = null;
        try {
            result = new SerialBlob(image);
            result.setBytes(1, image);
        } catch (SQLException e) {
            logger.error("Couldn't convert byte array to blob: ", e);
        }

        return result;
    }

    public byte[] convertBlobToByteArray(Blob blob) throws SQLException {
        byte[] result;
        result = blob.getBytes(1, (int) blob.length());
        return result;
    }

}
