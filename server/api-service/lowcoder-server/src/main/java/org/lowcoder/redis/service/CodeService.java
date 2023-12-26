package org.lowcoder.redis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;



@Service
public class CodeService {


    private RedisTemplate<String,String> stringRedisTemplate;


    @Autowired
    public void setStringRedisTemplate(RedisTemplate<String, String> stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // Generate and store code and name in Redis
    public void generateAndStoreCodeInRedis(String name, String codeValue) {
        stringRedisTemplate.opsForValue().set(name, codeValue, 5, TimeUnit.MINUTES);
    }

    //  take  Redis a codeValue
    public String getCodeFromRedis(String name) {
        return stringRedisTemplate.opsForValue().get(name);
    }

    //Verify if the user-inputted codeValue matches
    public Mono<Boolean> verifyNameAndCode(String name, String inputCode) {

        return Mono.fromCallable(() -> {
            //take codeValue from redis
            String storedCodeValue = getCodeFromRedis(name);

            if (storedCodeValue == null) {
                return false;
            }

            return storedCodeValue.equals(inputCode);
        });
    }
}
