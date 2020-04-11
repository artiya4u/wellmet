package com.bootlegsoft.wellmet;


import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;


public class Utils {

    private static final String CT_DTK = "CT-DTK";
    private static final String CT_RPI = "CT-RPI";

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(x);
        return buffer.array();
    }

    public static String genTracingKey(String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes()); // Add more uniqueness
            Random randomNo = new Random();
            byte[] ikm = new byte[32]; // 256 bits
            randomNo.nextBytes(ikm);
            md.update(ikm);
            byte[] digest = md.digest();
            return Utils.bytesToHex(digest);
        } catch (NoSuchAlgorithmException cnse) {
            return null;
        }
    }

    public static byte[] genDailyTracingKey(byte[] tracingKey, long dayNumber) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(tracingKey);
            md.update(CT_DTK.getBytes(StandardCharsets.UTF_8));
            md.update(longToBytes(dayNumber));
            byte[] digest = md.digest();
            // Truncate to 16 bytes
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            UUID dailyUUID = UUID.nameUUIDFromBytes(digest);
            bb.putLong(dailyUUID.getMostSignificantBits());
            bb.putLong(dailyUUID.getLeastSignificantBits());
            return bb.array();
        } catch (NoSuchAlgorithmException cnse) {
            return null;
        }
    }

    public static UUID genRollingProximityIdentifier(byte[] dailyTracingKey, long timeIntervalNumber) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(dailyTracingKey);
            md.update(CT_RPI.getBytes(StandardCharsets.UTF_8));
            md.update(longToBytes(timeIntervalNumber));
            byte[] digest = md.digest();
            // Truncate to UUID
            return UUID.nameUUIDFromBytes(digest);
        } catch (NoSuchAlgorithmException cnse) {
            return null;
        }
    }

    public static String shortenUUID(String uuid) {
        if (uuid.length() == 36) {
            return uuid.substring(0, 8) + "..." + uuid.substring(24);
        } else {
            return uuid;
        }
    }

}
