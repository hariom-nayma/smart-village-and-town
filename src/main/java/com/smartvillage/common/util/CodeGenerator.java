package com.smartvillage.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

public final class CodeGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CodeGenerator() {}

    public static String generateOrderNumber() {
        String date = LocalDate.now().format(DATE_FORMAT);
        int rand = 100000 + RANDOM.nextInt(900000);
        return "ORD-" + date + "-" + rand;
    }

    public static String generateDeliveryOtp() {
        int otp = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    public static String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static String generateServiceBookingNumber() {
        String date = LocalDate.now().format(DATE_FORMAT);
        int rand = 100000 + RANDOM.nextInt(900000);
        return "SVSB-" + date + "-" + rand;
    }

    public static String generateTransportBookingNumber() {
        String date = LocalDate.now().format(DATE_FORMAT);
        int rand = 100000 + RANDOM.nextInt(900000);
        return "TR-" + date + "-" + rand;
    }

    public static String generateComplaintTrackingId() {
        String date = LocalDate.now().format(DATE_FORMAT);
        int rand = 1000 + RANDOM.nextInt(9000);
        return "CMPT-" + date + "-" + rand;
    }

    public static String generateCustomerPin() {
        int pin = 10000000 + RANDOM.nextInt(90000000);
        return String.valueOf(pin);
    }

    public static String generateCompletionCode() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
}
