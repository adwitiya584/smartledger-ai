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
    // Store OTP in memory: email -> {otp, expiry}
    private final Map<String, long[]> otpStore = new ConcurrentHashMap<>();

    public OtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String email) {
        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        long expiry = System.currentTimeMillis() + (10 * 60 * 1000); // 10 mins

        otpStore.put(email, new long[]{Long.parseLong(otp), expiry});

        // Send email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Paisa Nest — Email Verification OTP");
        message.setText(
            "Welcome to Paisa Nest!\n\n" +
            "Your OTP for registration is: " + otp + "\n\n" +
            "This OTP is valid for 10 minutes.\n\n" +
            "If you didn't request this, please ignore this email.\n\n" +
            "— Paisa Nest Team"
        );
        mailSender.send(message);
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
            otpStore.remove(email); // OTP used — remove it
            return true;
        }
        return false;
    }

    public boolean isEmailPendingVerification(String email) {
        return otpStore.containsKey(email);
    }
}