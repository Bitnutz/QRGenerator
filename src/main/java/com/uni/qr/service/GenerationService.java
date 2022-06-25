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

import javax.sql.rowset.serial.SerialBlob;
import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
        Log currentLog = new Log();

        if(vanillaUrl.contains("/")) {
            currentLog.setAction("Cannot parse detailed URLs, which contain paths.");
            currentLog.setTimestamp(currentTimestamp);
            URL unusableUrl = new URL();
            unusableUrl.setTimestamp(new Timestamp(System.currentTimeMillis()));
            unusableUrl.setResult(null);
            unusableUrl.setDescription("unusable");
            unusableUrl.setVanilla_URL("www.unusable.com");
            unusableUrl.setDeleteFlag(true);
            return unusableUrl;
        }
        URL urlObject = new URL();
        urlObject.setVanilla_URL(vanillaUrl);
        urlObject.setDescription(description);
        urlObject.setDeleteFlag(false);
        urlObject.setTimestamp(currentTimestamp);
        urlObject.setResult(convertByteArrayToBlob(GenerationService.getQRCodeImage(vanillaUrl, width,height)));

        urlRepository.save(urlObject);


        currentLog.setAction("inserted a row into URL table!");
        currentLog.setTimestamp(currentTimestamp);

        logRepository.save(currentLog);
        return urlObject;
    }

    @Transactional
    public InputStream getQrFinal(Integer uid) {
        Log currentLog = new Log();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        if(!urlRepository.findById(uid).isPresent()) {
            currentLog.setAction("Tried to access a row, which does not exists");
            currentLog.setTimestamp(timestamp);
            logger.error(String.format("No URL found with the given uid: %d%n", uid));
            return null;
        }
        if(urlRepository.findById(uid).get().isDeleteFlag()) {
            currentLog.setAction("Tried to access a row, which was deleted");
            currentLog.setTimestamp(timestamp);
            logger.error(String.format("The URL with the uid: %d was already deleted\n", uid));
            return null;
        }
        currentLog.setAction(String.format("Fetched the QR code with uid: %d", uid));
        currentLog.setTimestamp(timestamp);
        logRepository.save(currentLog);
        URL currentUrl = urlRepository.findById(uid).get();

        byte[] imageBytes = new byte[0];
        try {
            imageBytes = IOUtils.toByteArray(currentUrl.getResult().getBinaryStream());
        } catch (IOException | SQLException e) {
            logger.error("Unable to convert BLOB to byte array: ", e);
        }
        return new ByteArrayInputStream(imageBytes);
    }

    @Transactional
    public String deleteGeneratedQr(Integer uid) {
        Log currentLog = new Log();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());


        if(!urlRepository.findById(uid).isPresent()) {
            currentLog.setAction("Tried to delete a row, which does not exists");
            currentLog.setTimestamp(timestamp);
            logRepository.save(currentLog);
            return currentLog.getAction();
        }
        if(urlRepository.findById(uid).get().isDeleteFlag()) {
            currentLog.setAction("Tried to delete a row, which is already deleted");
            currentLog.setTimestamp(timestamp);
            logRepository.save(currentLog);
            return currentLog.getAction();
        }
        URL currentUrl = urlRepository.findById(uid).get();
        currentUrl.setDeleteFlag(true);
        urlRepository.save(currentUrl);
        return String.format("Deleted UID: %d", uid);
    }

    @Transactional
    public byte[] getQr(Integer uid) throws SQLException {

        Log currentLog = new Log();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        if(!urlRepository.findById(uid).isPresent()) {
            currentLog.setAction("Tried to access a row, which does not exists");
            currentLog.setTimestamp(timestamp);
            return null;
        }
        if(urlRepository.findById(uid).get().isDeleteFlag()) {
            currentLog.setAction("Tried to access a row, which was deleted");
            currentLog.setTimestamp(timestamp);
            return null;
        }
        currentLog.setAction(String.format("Fetched the QR code with uid: %d", uid));
        currentLog.setTimestamp(timestamp);
        logRepository.save(currentLog);
        URL currentUrl = urlRepository.findById(uid).get();
        System.out.println(currentUrl.getResult().getBinaryStream());

        byte[] imageBytes = new byte[0];
        try {
            imageBytes = getImage(uid);
        } catch (IOException | SQLException e) {
            logger.error("Unable to convert inputStream to byte array while fetching the QR in GenerationService.getQr(uid): ", e);
        }

        return imageBytes;
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
}
