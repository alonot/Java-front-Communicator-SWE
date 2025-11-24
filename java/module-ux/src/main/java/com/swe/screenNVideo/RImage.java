/**
 *  Contributed by Sandeep Kumar.
 */

package com.swe.screenNVideo;



import java.nio.ByteBuffer;

/**
 * UIImage to be sent via RPC.
 */
public class RImage {
    /**
     * UIImage to be sent to UI.
     */
    private final int[][] image;

    /**
     * ip of the user whose image is this.
     */
    private final String ip;

    private RImage(final int[][] imageArgs, final String ipArgs) {
        ip = ipArgs;
        image = imageArgs;
    }

    /**
     * Deserializes the image.
     * @param data the byte array to be deserialized
     * @return the image.
     */
    public static RImage deserialize(final byte[] data) {
        final ByteBuffer buffer = ByteBuffer.wrap(data);

        // get the IP
        final int ipLen = buffer.getInt();
        final byte[] ipBytes = new byte[ipLen];
        final int bufferStart = 0;
        buffer.get(ipBytes, bufferStart, ipLen);
        final String ip = new String(ipBytes);

        // get the UIImage
        final int height = buffer.getInt();
        final int width = buffer.getInt();
        final int[][] image = new int[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                final int r = buffer.get() & Utils.BYTE_MASK;
                final int g = buffer.get() & Utils.BYTE_MASK;
                final int b = buffer.get() & Utils.BYTE_MASK;

                // ARGB pixel with full alpha (Utils.BYTE_MASK000000)
                final int pixel = (Utils.BYTE_MASK << Utils.INT_MASK_24) | (
                    r << Utils.INT_MASK_16) | (g << Utils.INT_MASK_8) | b;

                image[i][j] = pixel;
            }
        }
        return new RImage(image, ip);
    }

    public int[][] getImage() {
        return image;
    }

    public String getIp() {
        return ip;
    }
}
