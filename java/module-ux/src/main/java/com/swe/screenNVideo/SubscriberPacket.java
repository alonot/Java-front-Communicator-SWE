/**
 *  Contributed by Sandeep Kumar.
 */

package com.swe.screenNVideo;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Subscribe Packet.
 * @param ip The IP address of the publisher.
 * @param reqCompression True if compression is requested, false otherwise.
 */
public record SubscriberPacket(String ip, boolean reqCompression) {

    /**
     * Byte value for true.
     */
    private static final byte BYTE_TRUE = 1;

    /**
     * Byte value for false.
     */
    private static final byte BYTE_FALSE = 0;

    /**
     * Serializes the string for networking layer.
     * @return serialized byte array
     */
    public byte[] serialize() {
        final int len = 4 * Integer.BYTES + 1; // 4 int for ip and one for boolean
        final ByteBuffer buffer = ByteBuffer.allocate(len + 1);
        buffer.put((byte) 0); // dummy to reuse a func in core

        final int[] ipInts;
        ipInts = Arrays.stream(ip.split("\\.")).mapToInt(Integer::parseInt).toArray();

        for (int ipInt : ipInts) {
            buffer.putInt(ipInt);
        }

        if (reqCompression) {
            buffer.put(BYTE_TRUE);
        } else {
            buffer.put(BYTE_FALSE);
        }

        return buffer.array();
    }

}
