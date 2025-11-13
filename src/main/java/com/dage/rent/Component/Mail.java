package com.dage.rent.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.lang.*;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;

import java.util.ArrayList;
import java.util.Properties;

@Component
public class Mail extends Authenticator {

    @Value("${spring.mail.username}")
    private String sendId;

    @Value("${spring.mail.password}")
    private String sendPw;


    private class SMTPAuthenticator extends Authenticator {
        public PasswordAuthentication getPasswordAuthentication() {
            String username = sendId; // gmail 사용자;
            String password = sendPw; // 패스워드;
            return new PasswordAuthentication(username, password);
        }
    }

    // 이메일 전송 메소드 - 다중 수신자 지원
    public void sendEmail(String[] recipients, String[] ccRecipients, String[] bccRecipients, String subject, String htmlContent) {

        Properties p = System.getProperties();
        p.put("mail.smtp.ssl.protocols", "TLSv1.2");
        p.put("mail.smtp.ssl.enable", "true");
        p.put("mail.smtp.ssl.trust", "smtp.mailplug.co.kr");
        p.put("mail.smtp.host", "smtp.mailplug.co.kr");      // smtp 서버 호스트
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.port", "465");                 //  포트

        Authenticator auth = new SMTPAuthenticator();    //계정 인증

        //session 생성 및  MimeMessage생성
        Session session = Session.getInstance(p, auth);

        try {

            Message message = new MimeMessage(session);

            // 발신자(from) 추가
            String fromName = "동아지질";
            InternetAddress from = new InternetAddress();
            message.setFrom(new InternetAddress(sendId,fromName));

            // 수신자(TO) 추가
            if (recipients != null && recipients.length > 0) {
                InternetAddress[] toAddresses = new InternetAddress[recipients.length];
                for (int i = 0; i < recipients.length; i++) {
                    toAddresses[i] = new InternetAddress(recipients[i]);
                }
                message.setRecipients(Message.RecipientType.TO, toAddresses);
            }

            // 참조(CC) 추가
            if (ccRecipients != null && ccRecipients.length > 0) {
                InternetAddress[] ccAddresses = new InternetAddress[ccRecipients.length];
                for (int i = 0; i < ccRecipients.length; i++) {
                    ccAddresses[i] = new InternetAddress(ccRecipients[i]);
                }
                message.setRecipients(Message.RecipientType.CC, ccAddresses);
            }

            // 숨은참조(BCC) 추가
            if (bccRecipients != null && bccRecipients.length > 0) {
                InternetAddress[] bccAddresses = new InternetAddress[bccRecipients.length];
                for (int i = 0; i < bccRecipients.length; i++) {
                    bccAddresses[i] = new InternetAddress(bccRecipients[i]);
                }
                message.setRecipients(Message.RecipientType.BCC, bccAddresses);
            }

            message.setSubject(subject);

            // HTML 내용 설정
            message.setContent(htmlContent, "text/html; charset=UTF-8");

            Transport.send(message);

            System.out.println("이메일이 성공적으로 전송되었습니다.");

        }catch (AddressException e) {  //예외처리 주소를 입력하지 않을 경우
            throw new RuntimeException(e);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<String> sendMail(String user, String text, String content) {

        ArrayList<String> send_list = new ArrayList<>();
        String send_tg = "T";
        String send_msg = "성공";

        Properties p = System.getProperties();

        p.put("mail.smtp.ssl.protocols", "TLSv1.2");
        p.put("mail.smtp.ssl.enable", "true");
        p.put("mail.smtp.ssl.trust", "smtp.mailplug.co.kr");
        p.put("mail.smtp.host", "smtp.mailplug.co.kr");      // smtp 서버 호스트
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.port", "465");                 //  포트

        Authenticator auth = new SMTPAuthenticator();    //계정 인증

        //session 생성 및  MimeMessage생성
        Session session = Session.getInstance(p, auth);
        String charSet = "UTF-8";

        try {

            MimeMessage msg = new MimeMessage(session);
            String fromName = "동아지질";
            System.out.println("메일 전송 중.");
            // 편지보낸시간 설정
            msg.setSentDate(new java.util.Date());

            // 발신자 설정
            InternetAddress from = new InternetAddress();

            msg.setFrom(new InternetAddress(sendId,fromName));
            // 수신자 설정
            InternetAddress to = new InternetAddress(user);
            msg.setRecipient(Message.RecipientType.TO, to);

            // 제목 설정
            msg.setSubject(text, "UTF-8");
            //내용 html태그
            msg.setContent(content,"text/html;  charset=euc-kr");

            //내용 설정 text
            /*msg.setText(content, "UTF-8"); */

            // 메일 송신
            Transport.send(msg);

            System.out.println("메일 발송을 완료하였습니다.");
            System.out.println("----------------------\n");
        } catch (AddressException addr_e) {  //예외처리 주소를 입력하지 않을 경우
            addr_e.printStackTrace();
            send_msg = "e-mail 주소확인";
            send_tg = "F";
        } catch (MessagingException msg_e) { //메시지에 이상이 있을 경우
            System.out.println(msg_e);
            msg_e.printStackTrace();
            send_msg = "전송메시지 오류";
            send_tg = "F";
        } catch (Exception e) {
            System.out.println(e);
            send_msg = String.valueOf(e);
            send_tg = "F";
        }

        send_list.add(0,send_tg);
        send_list.add(1,send_msg);

        return send_list;
    }

    public ArrayList<String> sendMail_multi(String user, String text, String content) {

        ArrayList<String> send_list = new ArrayList<>();

        String send_tg = "T";
        String send_msg = "성공";
        String charSet = "UTF-8";

        Properties properties = System.getProperties();

        properties.put("mail.smtp.ssl.protocols", "TLSv1.2");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.ssl.trust", "smtp.mailplug.co.kr");
        properties.put("mail.smtp.host", "smtp.mailplug.co.kr");      // smtp 서버 호스트
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.port", "465");                 //  포트

        Authenticator auth = new SMTPAuthenticator();    //계정 인증
        //session 생성 및  MimeMessage생성
        Session session = Session.getInstance(properties, auth);

        try {

            MimeMessage msg = new MimeMessage(session);
            String fromName = "동아지질";
            System.out.println("메일 전송 중.");
            // 편지보낸시간 설정
            msg.setSentDate(new java.util.Date());
            // 발신자 설정
            InternetAddress from = new InternetAddress();
            msg.setFrom(new InternetAddress(sendId,fromName));
            // 수신자 설정
            InternetAddress to = new InternetAddress(user);
            msg.setRecipient(Message.RecipientType.TO, to);
            // 제목 설정
            msg.setSubject(text, "UTF-8");

            // 메시지 본문 생성
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(content,"text/html; charset=euc-kr");

            // 메시지 본문을 포함할 Multipart 객체 생성
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            // 첨부파일 설정
//            messageBodyPart = new MimeBodyPart();
//            //String filename = "src/main/resources/static/images/temp.pdf"; // 첨부파일 경로
//            String filename = System.getProperty("user.dir")+"labs/temp.pdf";
//            DataSource source = new FileDataSource(filename);
//            messageBodyPart.setDataHandler(new DataHandler(source));
//            messageBodyPart.setFileName("근로계약서.pdf");
//            multipart.addBodyPart(messageBodyPart);

            // Multipart 객체를 메시지에 설정
            msg.setContent(multipart);

            // 메일 송신
            Transport.send(msg);

            System.out.println("메일 발송을 완료하였습니다.");
            System.out.println("----------------------\n");
        } catch (AddressException addr_e) {  //예외처리 주소를 입력하지 않을 경우
            addr_e.printStackTrace();
            send_msg = "e-mail 주소확인";
            send_tg = "F";
        } catch (MessagingException msg_e) { //메시지에 이상이 있을 경우
            System.out.println(msg_e);
            msg_e.printStackTrace();
            send_msg = "전송메시지 오류";
            send_tg = "F";
        } catch (Exception e) {
            System.out.println(e);
            send_msg = String.valueOf(e);
            send_tg = "F";
        }

        send_list.add(0,send_tg);
        send_list.add(1,send_msg);

        return send_list;
    }

}