package uk.co.instanto.client.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class RpcProtocol {

    public static final byte[] MAGIC_HEADER = { 'V', 'R', 'P', 'C', 1 };

    /**
     * Checks if the data starts with the RPC magic header.
     * @param data the incoming bytes
     * @return true if the data is a valid RPC packet, false otherwise
     */
    public static boolean isRpcPacket(byte[] data) {
        if (data == null || data.length < MAGIC_HEADER.length) {
            return false;
        }
        for (int i = 0; i < MAGIC_HEADER.length; i++) {
            if (data[i] != MAGIC_HEADER[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Wraps the payload with the RPC magic header.
     * @param payload the protobuf payload
     * @return a new byte array containing the header followed by the payload
     */
    public static byte[] wrap(byte[] payload) {
        byte[] packet = new byte[MAGIC_HEADER.length + payload.length];
        System.arraycopy(MAGIC_HEADER, 0, packet, 0, MAGIC_HEADER.length);
        System.arraycopy(payload, 0, packet, MAGIC_HEADER.length, payload.length);
        return packet;
    }

    /**
     * Creates an InputStream for decoding the payload, skipping the header.
     * Assumes isRpcPacket(data) is true.
     */
    public static InputStream getPayloadStream(byte[] data) {
        if (!isRpcPacket(data)) {
            throw new IllegalArgumentException("Invalid RPC packet");
        }
        return new ByteArrayInputStream(data, MAGIC_HEADER.length, data.length - MAGIC_HEADER.length);
    }
}
