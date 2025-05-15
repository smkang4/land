package com.dage.rent.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Map;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    public void sendApprovalEmail(String userId, String userName, String contractNo) throws MessagingException {
        String to = userId + "@dage.co.kr";
        
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("contractNo", contractNo);
        
        String emailContent = templateEngine.process("approval-email", context);
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom("dong-ah@dage.co.kr");
        helper.setTo(to);
        helper.setSubject("[부동산 물건 사전조사서] 승인완료 알림 메일");
        helper.setText(emailContent, true);
        
        mailSender.send(message);
    }

    public void sendRejectionEmail(String userId, String userName, String contractNo, String rejectReason) throws MessagingException {
        String to = userId + "@dage.co.kr";
        
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("contractNo", contractNo);
        context.setVariable("rejectReason", rejectReason);
        
        String emailContent = templateEngine.process("rejection-email", context);
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("dong-ah@dage.co.kr");
        helper.setTo(to);
        helper.setSubject("[부동산 물건 사전조사서] 반려 알림 메일");
        helper.setText(emailContent, true);
        
        mailSender.send(message);
    }
} 