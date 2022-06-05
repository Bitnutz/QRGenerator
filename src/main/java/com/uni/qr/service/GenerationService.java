package com.uni.qr.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Service
public class GenerationService {
    public static void generateCode(String text, int width, int height, String path) throws WriterException, IOException {
        QRCodeWriter writer = new QRCodeWriter();

        BitMatrix bitMatrix=writer.encode(text, BarcodeFormat.QR_CODE,width,height);

        File file;
        file = File.createTempFile("temp", "txt");
        MatrixToImageWriter.writeToPath(bitMatrix,"PNG",file.toPath());
    }
}
