package org.lowcoder.email.controller;


import lombok.extern.slf4j.Slf4j;
import org.lowcoder.api.framework.view.ResponseView;
import org.lowcoder.domain.user.service.UserService;
import org.lowcoder.email.mail.service.impl.IMailServiceImpl;
import org.lowcoder.infra.constant.NewUrl;
import org.lowcoder.infra.constant.Url;
import org.lowcoder.redis.service.CaptchaService;
import org.lowcoder.redis.service.CodeService;
import org.lowcoder.redis.service.RegisterService;
import org.lowcoder.sdk.exception.BizError;
import org.lowcoder.sdk.exception.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.lowcoder.sdk.exception.BizError.*;
import static org.lowcoder.sdk.util.ExceptionUtils.ofError;


@Slf4j
@RestController
@RequestMapping(value = {Url.USER_URL, NewUrl.USER_URL})
public class EmailController {

    @Autowired
    private UserService userService;

    @Autowired
    private IMailServiceImpl iMailService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private CodeService codeService;

   @Autowired
   private RegisterService registerService;

    // ========================== REGISTER USER START ==========================

    @PostMapping("/sendRegisterMail")
    public Mono<ResponseEntity<ResponseView<String>>> sendVerificationCode(@RequestBody RegisterRequest registerRequest) {
        String name = registerRequest.name();
            // Send the registration email for the provided name
        String subject = "尚云数智帐号邮箱验证码";
        String content = "<strong>尊敬的用户：</strong> <br/><br/>"
                + "您好！<br/><br/>";

        String result = iMailService.sendRegisterMail(name, subject, content);
        if ("Email sent successfully".equals(result)) {
            ResponseView<String> response = ResponseView.lxSuccess(ResponseView.SUCCESS,"邮件发送成功","");
            return Mono.just(ResponseEntity.ok().body(response));
        } else {
            return Mono.error(new BizException(SEND_EMAIL_FAILED, "SEND_EMAIL_FAILED"));
        }
    }

    @PostMapping("/verifyRegisterCode")
    public Mono<ServerResponse> verifyRegisterCode(@RequestBody VerifyRegisterRequest verifyRequest) {
        String name = verifyRequest.name();
        String inputCode = verifyRequest.inputCode();
        return registerService.verifyNameAndCode(name, inputCode)
                .flatMap(NameAndCodeValidFirst -> {
                    if (NameAndCodeValidFirst) {
                        // success
                        return ServerResponse.ok().bodyValue("Verification successful");
                    } else {
                        // fails
                        return Mono.error(new BizException(VERIFICATION_FAILED, "VERIFICATION_FAILED"));
                    }
                });
    }

    // ========================== REGISTER USER END ==========================

    // ========================== RESET PASSWORD START ==========================

    @PostMapping("/sendResetPasswordEmail")
    public Mono<ResponseEntity<ResponseView<String>>> sendResetPasswordEmail(@RequestBody SendResetRequest resetRequest) {
        String name = resetRequest.name();
        String userInputCaptcha = resetRequest.inputCode();


        //  Verify if they consistent
        boolean isCaptchaValid = captchaService.verifyCaptcha(userInputCaptcha);

        if (isCaptchaValid) {
            // Select user by name
            return userService.findByName(name)
                    .flatMap(user -> {
                        if (user != null) {
                            String subject = "尚云数智帐号密码重置";

                            String content = "<strong>尊敬的用户：</strong> <br/><br/>"
                                    + "您好！<br/><br/>";

                            // If user exists, send the reset password email to the user's email address
                            iMailService.sendResetMail(user.getName(), subject, content);
                            ResponseView<String> response = ResponseView.lxSuccess(ResponseView.SUCCESS,"邮件发送成功","");
                            return Mono.just(ResponseEntity.ok().body(response));
                        } else {
                            // If user doesn't exist, return user not exist
                            return Mono.error(new BizException(USER_NOT_EXIST, "USER_NOT_EXIST"));
                        }
                    })
                    // If no user found for the given name , so user not exist
                    .switchIfEmpty(Mono.error(new BizException(USER_NOT_EXIST, "USER_NOT_EXIST")));
        } else {
            // If captcha validation fails, return  fail
            return Mono.error(new BizException(VERIFICATION_FAILED, "VERIFICATION_FAILED"));
        }
    }

//    @PostMapping("/verifyResetCode")
//    public Mono<ServerResponse> verifyResetCode(@RequestBody VerifyResetRequest resetRequest) {
//        String name = resetRequest.name();
//        String inputCode = resetRequest.inputCode();
//
//        return codeService.verifyNameAndCode(name, inputCode)
//                .flatMap(NameAndCodeValidFirst -> {
//                    if (NameAndCodeValidFirst) {
//                        // success
//                        return ServerResponse.ok().build();
//                    } else {
//                        // fails
//                        return Mono.error(new BizException(VERIFICATION_FAILED, "VERIFICATION_FAILED"));
//                    }
//                });
//    }


//    @PostMapping("/resetPassword")
//    public Mono<ResponseView<String>> resetPassword(@RequestBody ResetPasswordRequest resetRequest) {
//        String name = resetRequest.name();
//        String newPassword = resetRequest.newPassword();
//
//        return userService.findByName(name)
//                .flatMap(user -> {
//                    if (user != null) {
//                        return userService.resetPasswordByName(name, newPassword)
//                                .flatMap(result -> {
//                                    // true
//                                    String subject = "尚云数智帐号密码重置成功";
//                                    String content = "<strong>尊敬的用户：</strong> <br/><br/>"
//                                            + "您好！<br/><br/>";
//
//                                    iMailService.sendResetSuccess(user.getName(), subject, content);
//                                    return
////
//                                })
//                                .onErrorResume(error -> {
//                                    // false
//                                    return ofError(BizError.PASSWORD_RESET_FAILURE, "PASSWORD_RESET_FAILURE");
//                                });
//                    } else {
//                        // If user doesn't exist, return user not exist
//                        return ofError(BizError.USER_NOT_EXIST, "USER_NOT_EXIST");
//                    }
//                })
//                // If no user found for the given name , so user not exist
//                .switchIfEmpty(ofError(BizError.USER_NOT_EXIST, "USER_NOT_EXIST"));
//
//    }

    @PostMapping("/resetPassword")
    public Mono<ResponseEntity<ResponseView<String>>> resetPassword(@RequestBody ResetPasswordRequest resetRequest) {
        String name = resetRequest.name();
        String newPassword = resetRequest.newPassword();

        return userService.findByName(name)
                .flatMap(user -> {
                    if (user != null) {
                        return userService.resetPasswordByName(name, newPassword)
                                .flatMap(result -> {
                                    String subject = "尚云数智帐号密码重置成功";
                                    String content = "<strong>尊敬的用户：</strong> <br/><br/>"
                                            + "您好！<br/><br/>";

                                    iMailService.sendResetSuccess(user.getName(), subject, content);
                                    ResponseView<String> response = ResponseView.lxSuccess(ResponseView.SUCCESS,"重置密码成功","");
                                    return Mono.just(ResponseEntity.ok().body(response));
                                })
                                .onErrorResume(error -> {
                                    return Mono.error(new BizException(SEND_EMAIL_FAILED, "SEND_EMAIL_FAILED"));
                                });
                    } else {
                        return Mono.error(new BizException(USER_NOT_EXIST, "USER_NOT_EXIST"));
                    }
                })
                .switchIfEmpty(Mono.error(new BizException(USER_NOT_EXIST, "USER_NOT_EXIST")));
    }


    // ========================== RESET PASSWORD END ==========================
    public record RegisterRequest(String name) {
    }
    public record VerifyRegisterRequest(String name,String inputCode) {
    }

    // ========================== REGISTER USER END ==========================
    public record SendResetRequest(String name,String inputCode) {
    }
    public record VerifyResetRequest(String name,String inputCode) {
    }
    public record ResetPasswordRequest(String name,String newPassword) {
    }
    // ========================== RESET PASSWORD END ==========================
}