package org.lowcoder.redis.service;


import org.springframework.stereotype.Service;

@Service
public class CaptchaImageStorageService {


    private static String base64Image = "";

    public static String getBase64Image() {
        return base64Image;
    }

    public static void setBase64Image(String image) {
        base64Image = image;
    }
}
