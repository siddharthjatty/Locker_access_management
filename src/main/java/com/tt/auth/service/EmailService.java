package com.tt.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send OTP to the user's email address with a styled HTML template.
     */
    public void sendOtpEmail(String toEmail, String otp, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("\uD83D\uDD10 Your Two-Factor Authentication Code");
            helper.setText(buildOtpEmailBody(otp, userName), true);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send OTP email. Please try again.", e);
        }
    }

    private String buildOtpEmailBody(String otp, String userName) {
        return "<!DOCTYPE html>"
            + "<html><head><style>"
            + "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #0f172a; color: #e2e8f0; margin: 0; padding: 0; }"
            + ".container { max-width: 480px; margin: 40px auto; background: #1e293b; border-radius: 16px; overflow: hidden; box-shadow: 0 20px 60px rgba(0,0,0,0.5); }"
            + ".header { background: linear-gradient(135deg, #6366f1, #8b5cf6, #a855f7); padding: 32px; text-align: center; }"
            + ".header h1 { margin: 0; font-size: 22px; color: white; letter-spacing: 1px; }"
            + ".header p { margin: 8px 0 0; color: rgba(255,255,255,0.85); font-size: 14px; }"
            + ".body-content { padding: 32px; text-align: center; }"
            + ".otp-box { background: #0f172a; border: 2px solid #6366f1; border-radius: 12px; padding: 20px; margin: 24px 0; }"
            + ".otp-code { font-size: 36px; letter-spacing: 12px; font-weight: 700; color: #818cf8; font-family: 'Courier New', monospace; }"
            + ".warning { background: rgba(234, 179, 8, 0.1); border: 1px solid rgba(234, 179, 8, 0.3); border-radius: 8px; padding: 12px; margin-top: 20px; font-size: 13px; color: #fbbf24; }"
            + ".footer { padding: 20px 32px; text-align: center; border-top: 1px solid #334155; font-size: 12px; color: #64748b; }"
            + "</style></head><body>"
            + "<div class='container'>"
            + "<div class='header'><h1>\uD83D\uDD10 Two-Factor Authentication</h1><p>Secure login verification</p></div>"
            + "<div class='body-content'>"
            + "<p>Hello <strong>" + userName + "</strong>,</p>"
            + "<p>Use the following code to complete your login:</p>"
            + "<div class='otp-box'><div class='otp-code'>" + otp + "</div></div>"
            + "<div class='warning'>\u23F1\uFE0F This code expires in <strong>5 minutes</strong>.<br>Do not share this code with anyone.</div>"
            + "</div>"
            + "<div class='footer'>If you didn't request this code, please ignore this email.<br>&copy; 2024 Two-Factor Auth System</div>"
            + "</div></body></html>";
    }
}
