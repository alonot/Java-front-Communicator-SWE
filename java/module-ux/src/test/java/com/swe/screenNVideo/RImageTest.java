package com.swe.screenNVideo;

import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;

class RImageTest {

    @Test
    void testDeserialize() {
        // prepare test image data
        String testIp = "127.0.0.1";
        int width = 2;
        int height = 2;
        int[][] expectedPixels = {
                {0xFFFF0000, 0xFF00FF00},
                {0xFF0000FF, 0xFFFFFFFF}
        };

        // build byte array in RImage format: IP_LEN + IP + HEIGHT + WIDTH + RGB bytes
        byte[] ipBytes = testIp.getBytes();
        int bufferSize = 4 + ipBytes.length + 4 + 4 + (height * width * 3);
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

        buffer.putInt(ipBytes.length);
        buffer.put(ipBytes);
        buffer.putInt(height);
        buffer.putInt(width);

        // write RGB bytes (alpha is handled by RImage)
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int pixel = expectedPixels[i][j];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
                buffer.put((byte) (pixel & 0xFF));         // B
            }
        }

        RImage result = RImage.deserialize(buffer.array());

        assertNotNull(result);
        assertEquals(testIp, result.getIp());

        int[][] resultImage = result.getImage();
        assertEquals(height, resultImage.length);
        assertEquals(width, resultImage[0].length);

        // alpha is added by RImage, so values should match expected with alpha
        assertEquals(expectedPixels[0][0], resultImage[0][0], "Pixel (0,0) should be Red");
        assertEquals(expectedPixels[0][1], resultImage[0][1], "Pixel (0,1) should be Green");
        assertEquals(expectedPixels[1][0], resultImage[1][0], "Pixel (1,0) should be Blue");
        assertEquals(expectedPixels[1][1], resultImage[1][1], "Pixel (1,1) should be White");
    }
}
