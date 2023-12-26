package org.lowcoder.redis.service;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.TimeUnit;



@Service
public class CaptchaService {


    private RedisTemplate<String, byte[]> redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, byte[]> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    @Autowired
    private  CaptchaImageStorageService captchaImageStorageService;

    // Generate and store captcha image in Redis
    public void generateAndStoreCaptchaInRedis(String redisKey) {

        // Create captcha image
        BufferedImage captchaImage = generateCaptchaImage(redisKey);

        // Convert image to byte array
        byte[] imageBytes = convertImageToByteArray(captchaImage);

        String base64Image = convertImageToBase64(captchaImage);

        // Store image bytes in Redis with expiration time (e.g., 5 minutes)
        storeImageInRedis(redisKey, imageBytes, 5, TimeUnit.MINUTES);


    }

    // Convert BufferedImage to Base64 string
    private String convertImageToBase64(BufferedImage captchaImage) {
        try (ByteArrayOutputStream byteArrays = new ByteArrayOutputStream()) {
            ImageIO.write(captchaImage, "png", byteArrays);
            byte[] imageData = byteArrays.toByteArray();
            return Base64.getEncoder().encodeToString(imageData);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Generate a random captcha code
    public String generateCaptchaCode() {
        int length = 6; // 生成的随机码长度
        Random random = new Random();

        StringBuilder randomCodeBuilder = new StringBuilder(length);
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            randomCodeBuilder.append(randomChar);
        }
        return randomCodeBuilder.toString();
    }

    // Generate a random captcha code
    private BufferedImage generateCaptchaImage(String captchaCode) {
        // Create a BufferedImage object with specific width, height, and image type
        BufferedImage captchaImage = new BufferedImage(200, 50, BufferedImage.TYPE_INT_RGB);

        // Get graphics object from the image to draw on it
        Graphics2D graphics = captchaImage.createGraphics();

        // Set background color
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, captchaImage.getWidth(), captchaImage.getHeight());

        // Set text color and font
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("Arial", Font.BOLD, 30));

        // Draw code to image
        graphics.drawString(captchaCode, 50, 30);

        // release resources
        graphics.dispose();

        return captchaImage;
    }

    // Convert BufferedImage to byte array
    private byte[] convertImageToByteArray(BufferedImage captchaImage) {
        try (ByteArrayOutputStream byteArrays = new ByteArrayOutputStream()) {
            ImageIO.write(captchaImage, "png", byteArrays);
            return byteArrays.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    private void storeImageInRedis(String redisKey, byte[] imageBytes, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(redisKey, imageBytes, timeout, timeUnit);
    }


    public String getCaptchaImageFromRedis(String redisKey) {
        byte[] imageData = redisTemplate.opsForValue().get(redisKey);
        if (imageData != null) {
            return Base64.getEncoder().encodeToString(imageData);
        } else {
            return null;
        }
    }

    //Verify if the user-inputted captcha matches
    public boolean verifyCaptcha(String userInput) {
        // Get Base64 encoded image from storage
        String storedImage = CaptchaImageStorageService.getBase64Image();

        if (storedImage == null) {
            return false;
        }

        // Decode the stored Base64 image to byte array
        byte[] storedCaptchaBytes = Base64.getDecoder().decode(storedImage);

        // Generate a BufferedImage from the user input
        BufferedImage userInputCaptchaImage = generateCaptchaImage(userInput);

        // Convert BufferedImage to byte array
        byte[] userInputCaptchaImageBytes = convertImageToByteArray(userInputCaptchaImage);

        // Compare the byte arrays of the two captcha images
        return Arrays.equals(storedCaptchaBytes, userInputCaptchaImageBytes);
    }

}
