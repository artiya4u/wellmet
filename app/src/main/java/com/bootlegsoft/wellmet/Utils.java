package com.bootlegsoft.wellmet;


import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Random;
import java.util.UUID;


public class Utils {

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

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

    public static String genRandomSearchCode(String phoneNumber) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(phoneNumber.getBytes()); // Add more uniqueness
            Random randomNo = new Random();
            byte[] r = new byte[32]; // 256 bits
            randomNo.nextBytes(r);
            md.update(r);
            byte[] digest = md.digest();
            return Utils.bytesToHex(digest);
        } catch (NoSuchAlgorithmException cnse) {
            return null;
        }
    }

    public static UUID genBeaconUUID(byte[] searchKey, Date beaconStartTime) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(searchKey);
            md.update(longToBytes(beaconStartTime.getTime()));
            byte[] digest = md.digest();
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

    public static String getTimeAgo(long time) {
        if (time < 1000000000000L) {
            time *= 1000;
        }

        long now = System.currentTimeMillis();
        if (time > now || time <= 0) {
            return null;
        }


        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return "just now";
        } else if (diff < 2 * MINUTE_MILLIS) {
            return "a minute ago";
        } else if (diff < 50 * MINUTE_MILLIS) {
            return diff / MINUTE_MILLIS + " minutes ago";
        } else if (diff < 90 * MINUTE_MILLIS) {
            return "an hour ago";
        } else if (diff < 24 * HOUR_MILLIS) {
            return diff / HOUR_MILLIS + " hours ago";
        } else if (diff < 48 * HOUR_MILLIS) {
            return "yesterday";
        } else {
            return diff / DAY_MILLIS + " days ago";
        }
    }

}
