/**
 *  Contributed by Sandeep Kumar.
 */

package com.swe.ux.model;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Represents a video frame image received from a participant.
 * Contains the image data, source IP, and transmission status.
 */
public final class UIImage {
    /**
     * The actual image data to be displayed.
     */
    private final BufferedImage image;

    /**
     * The IP address of the sender.
     */
    private final String ip;

    /**
     * Status byte indicating if the image was successfully processed (1 for success, 0 for failure).
     */
    private byte isSuccess;

    /**
     * Constant representing a successful operation byte.
     */
    private static final byte BYTE_SUCCESS = 1;

    /**
     * Constant representing a failed operation byte.
     */
    private static final byte BYTE_FAILURE = 0;

    /**
     * Constructs a new UIImage object.
     * @param imageArg The buffered image data.
     * @param ipArg The source IP address.
     * @param isSuccessArg The initial success status.
     */
    public UIImage(
        final BufferedImage imageArg,
        final String ipArg,
        final byte isSuccessArg
    ) {
        this.image = imageArg;
        this.ip = ipArg;
        this.isSuccess = isSuccessArg;
    }

    /**
     * Sets the success status of the image transmission.
     * @param val True if successful, false otherwise.
     */
    public void setIsSuccess(final boolean val) {
        if (val) {
            isSuccess = BYTE_SUCCESS;
        } else {
            isSuccess = BYTE_FAILURE;
        }
    }

    public BufferedImage image() {
        return image;
    }

    public String ip() {
        return ip;
    }

    public byte isSuccess() {
        return isSuccess;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        final var that = (UIImage) obj;
        return Objects.equals(this.image, that.image)
                &&  Objects.equals(this.ip, that.ip)
                && this.isSuccess == that.isSuccess;
    }

    @Override
    public int hashCode() {
        return Objects.hash(image, ip, isSuccess);
    }

    @Override
    public String toString() {
        return "UIImage["
                + "image=" + image + ", "
                + "ip=" + ip + ", "
                + "isSuccess=" + isSuccess + ']';
    }

}
