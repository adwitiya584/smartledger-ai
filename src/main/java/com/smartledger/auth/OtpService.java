package com.smartledger.auth;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private final JavaMailSender mailSender;
    private final Map<String, long[]> otpStore = new ConcurrentHashMap<>();

    public OtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String email) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        long expiry = System.currentTimeMillis() + (10 * 60 * 1000);
        otpStore.put(email, new long[]{Long.parseLong(otp), expiry});

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("PaisaNest — Email Verification OTP");
            message.setText(
                "Welcome to PaisaNest!\n\n" +
                "Your OTP: " + otp + "\n\n" +
                "Valid for 10 minutes.\n\n" +
                "— PaisaNest Team"
            );
            mailSender.send(message);
            System.out.println("OTP sent successfully to: " + email);
        } catch (Exception e) {
            System.out.println("Mail sending failed: " + e.getMessage());
            // Don't throw — OTP is stored, just mail failed
            throw new RuntimeException("Failed to send email: " + e.getMessage());
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