package org.lowcoder.redis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Random;
import java.util.concurrent.TimeUnit;


@Service
public class RegisterService {


    private RedisTemplate<String,String> stringRedisTemplate;


    @Autowired
    public void setStringRedisTemplate(RedisTemplate<String, String> stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // Generate and store code and name in Redis
    public void generateAndStoreCodeInRedis(String name, String codeValue) {
        // redis 5  MINUTES
        stringRedisTemplate.opsForValue().set(name, codeValue,5,TimeUnit.MINUTES);
    }

    //  take  Redis a codeValue
    public String getCodeFromRedis(String name) {
        return stringRedisTemplate.opsForValue().get(name);
    }

    // Verify inputCode if consistent
    public Mono<Boolean> verifyNameAndCode(String name, String inputCode) {
        return Mono.fromCallable(() -> {
            String storedCodeValue = getCodeFromRedis(name);
            return storedCodeValue != null && storedCodeValue.equals(inputCode);
        });
    }

    public String generateRandomCode() {
        int length = 8;
        Random random = new Random();

        StringBuilder randomCodeBuilder = new StringBuilder(length);
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            randomCodeBuilder.append(randomChar);
        }
        System.out.println("code"+randomCodeBuilder.toString());
        return randomCodeBuilder.toString();
    }
}