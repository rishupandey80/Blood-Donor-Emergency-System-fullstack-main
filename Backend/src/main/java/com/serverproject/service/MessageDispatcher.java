
package com.serverproject.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MessageDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(MessageDispatcher.class);

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@bloodconnect.local}")
    private String mailFrom;

    @Value("${app.sms.default-country-code:+91}")
    private String defaultCountryCode;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private SmsSender smsSender;

    // EMAIL OTP - ENABLED
    @Async
    public void sendEmail(String to, String subject, String body) {

        if (to == null || to.isBlank() || to.endsWith(".invalid")) {
            return;
        }

        if (!mailEnabled || mailSender == null) {
            log.info("[EMAIL-SKIPPED] mail is disabled (to={})",
                    maskEmail(to));
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to.trim());
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            log.info("Email sent to {}", maskEmail(to));

        } catch (Exception e) {
            log.error("Email to {} failed: {}",
                    maskEmail(to), e.getMessage());
        }
    }

    // SMS OTP - TEMPORARILY DISABLED
    @Async
    public void sendSms(String phone, String text) {

        log.info("[SMS-SKIPPED] SMS temporarily disabled");

        return;
    }

    static String toE164(String phone, String defaultCc) {

        if (phone == null) return null;

        String p = phone.replaceAll("[\\s\\-().]", "");

        if (p.startsWith("00")) {
            p = "+" + p.substring(2);
        }

        if (p.startsWith("+")) {
            return p.matches("\\+\\d{8,15}") ? p : null;
        }

        if (p.matches("\\d{10}")) {
            return defaultCc + p;
        }

        if (p.matches("0\\d{10}")) {
            return defaultCc + p.substring(1);
        }

        if (p.matches("\\d{11,15}")) {
            return "+" + p;
        }

        return null;
    }

    private static String maskEmail(String email) {

        int at = email.indexOf('@');

        return at > 1
                ? email.charAt(0) + "***" + email.substring(at)
                : "***";
    }

    private static String maskPhone(String phone) {

        return phone.length() > 4
                ? "******" + phone.substring(phone.length() - 4)
                : "***";
    }
}
