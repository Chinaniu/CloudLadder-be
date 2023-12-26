package org.lowcoder.redis.controller;


import lombok.extern.slf4j.Slf4j;
import org.lowcoder.infra.constant.NewUrl;
import org.lowcoder.infra.constant.Url;
import org.lowcoder.redis.service.CaptchaImageStorageService;
import org.lowcoder.redis.service.CaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping(value = {Url.USER_URL, NewUrl.USER_URL})
public class CaptchaController {

    private final CaptchaService captchaService;

    @Autowired
    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }


    @GetMapping("/captcha")
    public ResponseEntity<String> generateCaptchaAndStoreInRedis() {
        String redisKey =  captchaService.generateCaptchaCode();
        captchaService.generateAndStoreCaptchaInRedis(redisKey);
        return ResponseEntity.ok(redisKey);
    }

    @GetMapping(value = "/captcha/{redisKey}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<String> getCaptchaImage(@PathVariable String redisKey) {
        String base64Image = captchaService.getCaptchaImageFromRedis(redisKey);
        if (base64Image != null) {
            CaptchaImageStorageService.setBase64Image(base64Image);
            String imageSrc = "data:image/png;base64," + base64Image;

            System.out.println(base64Image);
            return ResponseEntity.ok(imageSrc);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}