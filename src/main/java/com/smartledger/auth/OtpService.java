package com.smartledger.auth;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.*;
import com.sendgrid.helpers.mail.objects.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    @Value("${sendgrid.api-key:placeholder}")
    private String sendGridApiKey;

    @Value("${sendgrid.from-email:noreply@paisanest.com}")
    private String fromEmail;

    private final Map<String, long[]> otpStore = new ConcurrentHashMap<>();

    public void sendOtp(String email) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        long expiry = System.currentTimeMillis() + (10 * 60 * 1000);
        otpStore.put(email, new long[]{Long.parseLong(otp), expiry});

        try {
            Email from = new Email(fromEmail, "PaisaNest");
            Email to = new Email(email);
            String subject = "PaisaNest — Your Verification OTP";

            String htmlContent =
                "<div style='font-family:sans-serif;max-width:500px;margin:0 auto;padding:30px;background:#f8fafc;border-radius:12px'>" +
                "<div style='text-align:center;margin-bottom:24px'>" +
                "<h1 style='color:#6366f1;margin:0'>💰 PaisaNest</h1>" +
                "<p style='color:#64748b;margin:8px 0 0'>Your trusted finance companion</p>" +
                "</div>" +
                "<div style='background:#fff;padding:24px;border-radius:8px;text-align:center'>" +
                "<p style='color:#1e293b;font-size:16px'>Your verification code is:</p>" +
                "<div style='background:#f1f5f9;padding:16px;border-radius:8px;margin:16px 0'>" +
                "<span style='font-size:48px;font-weight:bold;color:#6366f1;letter-spacing:12px'>" + otp + "</span>" +
                "</div>" +
                "<p style='color:#64748b;font-size:14px'>Valid for <strong>10 minutes</strong></p>" +
                "</div>" +
                "<p style='color:#94a3b8;font-size:12px;text-align:center;margin-top:16px'>" +
                "If you didn't request this, please ignore this email." +
                "</p>" +
                "</div>";

            Content content = new Content("text/html", htmlContent);
            Mail mail = new Mail(from, subject, to, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);
            System.out.println("SendGrid status: " + response.getStatusCode());
            System.out.println("SendGrid body: " + response.getBody());

            if (response.getStatusCode() >= 400) {
                throw new RuntimeException("SendGrid error: " + response.getBody());
            }

            System.out.println("OTP sent successfully to: " + email);

        } catch (IOException e) {
            System.out.println("SendGrid failed: " + e.getMessage());
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }

    public boolean verifyOtp(String email, String otp) {
        if (!otpStore.containsKey(email)) return false;
        long[] stored = otpStore.get(email);
        long storedOtp = stored[0];
        long expiry = stored[1];
        if (System.currentTimeMillis() > expiry) {
            otpStore.remove(email);
            return false;
        }
        if (Long.parseLong(otp) == storedOtp) {
            otpStore.remove(email);
            return true;
        }
        return false;
    }
}