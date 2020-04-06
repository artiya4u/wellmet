package com.bootlegsoft.wellmet;


import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;


public class Utils {
    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(x);
        return buffer.array();
    }

    public static UUID genBeaconUUID(byte[] wellKey, Date beaconStartTime) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(wellKey);
            md.update(longToBytes(beaconStartTime.getTime()));
            byte[] digest = md.digest();
            return UUID.nameUUIDFromBytes(digest);
        } catch (NoSuchAlgorithmException cnse) {
            return null;
        }
    }
}
