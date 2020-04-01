package net.alea.beaconsimulator.bluetooth;

import android.bluetooth.le.ScanRecord;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.alea.beaconsimulator.bluetooth.model.IBeacon;

import java.nio.ByteBuffer;
import java.util.UUID;

public class IBeaconParser implements AdvertiseDataParser<IBeacon> {

    public final static byte SUB_TYPE = (byte) 0x02;
    public final static byte SUB_TYPE_LEN = (byte) 0x15;
    public final static int MANUFACTURER_PACKET_SIZE = 23;

    @Nullable
    @Override
    public IBeacon parseScanRecord(@NonNull ScanRecord scanRecord) {
        // Check data validity
        final SparseArray<byte[]> manufacturers = scanRecord.getManufacturerSpecificData();
        if (manufacturers == null || manufacturers.size() != 1) {
            return null;
        }
        final byte[] data = manufacturers.valueAt(0);
        if (data.length != MANUFACTURER_PACKET_SIZE) {
            return null;
        }
        final ByteBuffer buffer = ByteBuffer.wrap(data);
        final short beaconType = buffer.get();
        if (beaconType != SUB_TYPE) {
            return null;
        }
        final short beaconCode = buffer.get();
        if (beaconCode != SUB_TYPE_LEN) {
            return null;
        }
        // Parse data
        final long uuidHigh = buffer.getLong();
        final long uuidLow = buffer.getLong();
        final int major =  ByteTools.toIntFromShortInBytes_BE(new byte[]{buffer.get(), buffer.get()});
        final int minor = ByteTools.toIntFromShortInBytes_BE(new byte[]{buffer.get(), buffer.get()});
        final byte power = buffer.get();
        final IBeacon iBeacon = new IBeacon();
        iBeacon.setProximityUUID(new UUID(uuidHigh, uuidLow));
        iBeacon.setMajor(major);
        iBeacon.setMinor(minor);
        iBeacon.setPower(power);
        return iBeacon;
    }

}
