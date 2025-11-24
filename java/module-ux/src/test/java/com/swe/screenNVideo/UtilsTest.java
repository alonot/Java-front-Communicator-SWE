package com.swe.screenNVideo;

import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void testWriteInt() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int testValue = 0xAABBCCDD;

        Utils.writeInt(bos, testValue);

        byte[] result = bos.toByteArray();
        assertEquals(4, result.length);

        // check big-endian bytes
        assertEquals((byte) 0xAA, result[0]);
        assertEquals((byte) 0xBB, result[1]);
        assertEquals((byte) 0xCC, result[2]);
        assertEquals((byte) 0xDD, result[3]);
    }

    @Test
    void testConvertToRGBMatrix() {
        int width = 2;
        int height = 2;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // set two pixels
        int red = 0xFFFF0000;
        int blue = 0xFF0000FF;
        image.setRGB(0, 0, red);
        image.setRGB(1, 1, blue);

        int[][] matrix = Utils.convertToRGBMatrix(image);

        assertEquals(height, matrix.length);
        assertEquals(width, matrix[0].length);

        // compare pixels (masking just in case)
        assertEquals(red, matrix[0][0] & 0xFFFFFFFF);
        assertEquals(blue, matrix[1][1] & 0xFFFFFFFF);
    }

    @Test
    void testGetSelfIP() {
        // basic IP retrieval check
        String ip = Utils.getSelfIP();

        assertNotNull(ip);
        assertFalse(ip.isEmpty());

        // simple format check
        assertTrue(ip.matches("[0-9.:]+"));
    }

    @Test
    void testConstructor() {
        // cover default constructor
        assertNotNull(new Utils());
    }
}
