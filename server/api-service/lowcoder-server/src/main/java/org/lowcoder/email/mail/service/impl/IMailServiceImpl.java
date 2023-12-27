package org.lowcoder.email.mail.service.impl;

import freemarker.template.Configuration;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.lowcoder.email.mail.service.IMailService;
import org.lowcoder.redis.service.CaptchaService;
import org.lowcoder.redis.service.CodeService;
import org.lowcoder.redis.service.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Slf4j
@Service
public class IMailServiceImpl implements IMailService {


    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private CaptchaService captchaService;

    //Managing template configurations
    @Autowired
    private Configuration configuration;

    @Autowired
    private CodeService codeService;

    @Autowired
    private RegisterService registerService;

    //Configure email
    @Value("${spring.mail.from}")
    private String from;

    @Value("${urls.reset_password}")
    private String redirectLink;
    /**
     *  register
     * @param to  email
     * @param subject subject
     * @param content content

     */
    // ========================== REGISTER USER START ==========================
    @Override
    public String  sendRegisterMail(String to, String subject, String content) {
        //Retrieve MimeMessage object
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper messageHelper;

        try {
            messageHelper = new MimeMessageHelper(message,true);

            //send email people
            messageHelper.setFrom(from);

            //receive email people
            messageHelper.setTo(to);

            //email  subject
            messageHelper.setSubject(subject);

            // random code
            String randomCode = registerService.generateRandomCode();


            // create email
            String mailContent = content
                    + "验证码："
                    + "<strong>"
                    + randomCode
                    + "</strong>"
                    + "，用于尚云数智帐号"
                    + "<strong>"
                    +  to
                    + "</strong>"
                    + "密码，该验证码五分钟内有效。"
                    + "转给他人将导致帐号被盗和个人信息泄露，谨防诈骗。如非您操作请忽略。<br/><br/>"
                    + "此致<br/>"
                    + "尚云数智";

            //email content html
            messageHelper.setText(mailContent,true);

            //send email
            javaMailSender.send(message);

            registerService.generateAndStoreCodeInRedis(to, randomCode);

            //logic information
            return "Email sent successfully";
        } catch (MessagingException e) {
            log.error("Email sending failed", e);
            return "Email sending failed";
        }
    }

    // ========================== REGISTER USER END ==========================

    /**
     *
     * @param to  email
     * @param subject subject
     * @param content content
     */
    // ========================== REGISTER RESET PASSWORD START ==========================
    //code
    @Override
    public String  sendResetMail(String to, String subject, String content) {
        //Retrieve MimeMessage object
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper messageHelper;

        try {
            messageHelper = new MimeMessageHelper(message,true);

            //send email people
            messageHelper.setFrom(from);

            //receive email people
            messageHelper.setTo(to);

            //email  subject
            messageHelper.setSubject(subject);

            // random code
            String randomCode = registerService.generateRandomCode();

            // random code link
//            String redirectUrl = "http://localhost:8000/user/auth/resetPassword?resetCode=" + randomCode ;
            String redirectUrl = redirectLink + randomCode ;



            // email content link
            String link = "<a href=\"" + redirectUrl + "\">重置密码链接</a>";

            // create email
            String mailContent = content
                    + "您最近申请了重设密码，请点击下面的链接设置新密码：<br/><br/>"
                    + link
                    + "该链接在5分钟内有效，"
                    + "如果无法打开链接，请复制上面的链接粘贴到浏览器的地址栏。<br/>"
                    + "如果你未提交此次申请，或者不想重设密码，请忽略此邮件。<br/><br/>"
                    + "此致 </br>"
                    + "尚云数智";

            //email content html
            messageHelper.setText(mailContent,true);

            //send email
            javaMailSender.send(message);

            codeService.generateAndStoreCodeInRedis(to, randomCode);

            //logic information
            return "Email sent successfully";
        } catch (MessagingException e) {
            log.error("Email sending failed", e);
            return "Email sending failed";
        }
    }

    // ========================== REGISTER RESET PASSWORD END ==========================


    @Override
    public String sendResetSuccess(String to, String subject, String content) {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper messageHelper;

        try {
            messageHelper = new MimeMessageHelper(message,true);

            //send email people
            messageHelper.setFrom(from);

            //receive email people
            messageHelper.setTo(to);

            //email  subject
            messageHelper.setSubject(subject);


            //get current time
            LocalDateTime currentTime = LocalDateTime.now();

            // dateFormatter
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日HH:mm");
            String formattedDateTime = currentTime.format(formatter);


            // create email
            String mailContent = content
                    + "您的尚云数智帐号"
                    + "<strong>"
                    +  to
                    + "</strong>"
                    + "于"
                    + formattedDateTime
                    + "重置了密码。<br/>"
                    + "此致 </br>"
                    + "尚云数智";

            //email content html
            messageHelper.setText(mailContent,true);

            //send email
            javaMailSender.send(message);

            //logic information
            return "Email sent successfully";
        } catch (MessagingException e) {
            log.error("Email sending failed", e);
            return "Email sending failed";
        }
    }
}
