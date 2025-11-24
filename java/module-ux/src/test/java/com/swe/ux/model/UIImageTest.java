package com.swe.ux.model;

import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;

class UIImageTest {

    @Test
    void testGettersAndSetters() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        String ip = "192.168.1.1";
        byte success = 1;

        UIImage uiImage = new UIImage(img, ip, success);

        assertSame(img, uiImage.image());
        assertEquals(ip, uiImage.ip());
        assertEquals(success, uiImage.isSuccess());

        // toggle success flag
        uiImage.setIsSuccess(false);
        assertEquals((byte) 0, uiImage.isSuccess());

        uiImage.setIsSuccess(true);
        assertEquals((byte) 1, uiImage.isSuccess());
    }

    @Test
    void testEqualsComprehensive() {
        BufferedImage img1 = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        BufferedImage img2 = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        String ip1 = "127.0.0.1";
        String ip2 = "192.168.1.1";

        UIImage base = new UIImage(img1, ip1, (byte) 1);

        // identity
        assertTrue(base.equals(base));

        // null and type checks
        assertFalse(base.equals(null));
        assertFalse(base.equals("Not a UIImage"));

        // equal fields
        UIImage same = new UIImage(img1, ip1, (byte) 1);
        assertTrue(base.equals(same));

        // different image
        UIImage diffImage = new UIImage(img2, ip1, (byte) 1);
        assertFalse(base.equals(diffImage));

        // different ip
        UIImage diffIp = new UIImage(img1, ip2, (byte) 1);
        assertFalse(base.equals(diffIp));

        // different success
        UIImage diffSuccess = new UIImage(img1, ip1, (byte) 0);
        assertFalse(base.equals(diffSuccess));
    }

    @Test
    void testHashCode() {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        String ip = "1.1.1.1";

        UIImage obj1 = new UIImage(img, ip, (byte) 1);
        UIImage obj2 = new UIImage(img, ip, (byte) 1);

        assertEquals(obj1.hashCode(), obj2.hashCode());
    }

    @Test
    void testToString() {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        String ip = "localhost";
        UIImage uiImage = new UIImage(img, ip, (byte) 1);

        String str = uiImage.toString();

        assertNotNull(str);
        assertTrue(str.contains("UIImage"));
        assertTrue(str.contains("ip=localhost"));
        assertTrue(str.contains("isSuccess=1"));
    }
}
