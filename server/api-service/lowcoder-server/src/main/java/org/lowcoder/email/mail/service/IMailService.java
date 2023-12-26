package org.lowcoder.email.mail.service;


public interface IMailService {


    /**
     *
     *  registerMail
     *
     * @param to   email
     * @param subject subject
     * @param content content
     */
    String sendRegisterMail(String to,String subject,String content);


    /**
     *
     * resetMail
     *
     * @param to   email
     * @param subject subject
     * @param content content
     */
    String sendResetMail(String to,String subject,String content);


    /**
     *
     *  resetSuccess
     * @param to
     * @param subject
     * @param content
     * @return
     */
    String sendResetSuccess(String to,String subject,String content);

}
