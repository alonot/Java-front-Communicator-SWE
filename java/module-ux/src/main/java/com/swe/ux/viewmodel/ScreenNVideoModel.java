/**
 *  Contributed by Sandeep Kumar.
 */

package com.swe.ux.viewmodel;

import com.swe.controller.RPCinterface.AbstractRPC;
import com.swe.screenNVideo.RImage;
import com.swe.screenNVideo.SubscriberPacket;
import com.swe.screenNVideo.Utils;
import com.swe.ux.binding.BindableProperty;
import com.swe.ux.model.UIImage;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * ViewModel for managing the screen and video data logic.
 * Handles RPC subscriptions and participant visibility tracking.
 */
public class ScreenNVideoModel extends BaseViewModel {

    /**
     * Callback invoked when a new video frame is received.
     */
    private Consumer<UIImage> onImageReceived;

    /**
     * RPC interface for network communication.
     */
    private final AbstractRPC rpc;

    /**
     * Singleton instance of the model.
     */
    private static ScreenNVideoModel instance;

    /**
     * Value for successful image processing.
     */
    private static final byte BYTE_SUCCESS = 1;

    /**
     * Property to track visible participants (IPs).
     */
    public final BindableProperty<Set<String>> visibleParticipants = new BindableProperty<>(
            new HashSet<>(), "visibleParticipants"
    );

    /**
     * Gets the singleton instance of the model.
     * @param rpc The RPC interface to use if creating a new instance.
     * @return The ScreenNVideoModel instance.
     */
    public static ScreenNVideoModel getInstance(final AbstractRPC rpc) {
        if (instance == null) {
            instance = new ScreenNVideoModel(rpc);
        }
        return instance;
    }

    private ScreenNVideoModel(final AbstractRPC rpcArg) {
        this.rpc = rpcArg;
        
        initComponents();
    }

    /**
     * Sets the callback for receiving image frames.
     * @param onImageReceivedArg The consumer to accept new UIImages.
     */
    public void setOnImageReceived(final Consumer<UIImage> onImageReceivedArg) {
        this.onImageReceived = onImageReceivedArg;
    }

    public void requestUncompressedData(final String ip) {
        final SubscriberPacket subscriberPacket = new SubscriberPacket(ip, false);
        rpc.call(Utils.SUBSCRIBE_AS_VIEWER, subscriberPacket.serialize());
    }

    public void requestCompressedData(final String ip) {
        final SubscriberPacket subscriberPacket = new SubscriberPacket(ip, true);
        rpc.call(Utils.SUBSCRIBE_AS_VIEWER, subscriberPacket.serialize());
    }

    /**
     * Updates the list of currently visible participants.
     * Called by the View when layout or scroll changes.
     * @param visibleEmails Set of visible email IDs/IPs.
     */
    public void updateVisibleParticipants(final Set<String> visibleEmails) {
        System.out.println("Participants " + Arrays.toString(visibleEmails.toArray()));
        // get new ips
        for (String emails : visibleEmails) {
            if (!visibleParticipants.get().contains(emails)) {
                final SubscriberPacket subscriberPacket = new SubscriberPacket(emails, true);
                try {
                    rpc.call(Utils.SUBSCRIBE_AS_VIEWER, subscriberPacket.serialize());
                } catch (NumberFormatException ignored) {

                }
            }
        }
        // get ips to remove
        for (String ip : visibleParticipants.get()) {
            if (!visibleEmails.contains(ip)) {
                final SubscriberPacket subscriberPacket = new SubscriberPacket(ip, true);
                try {
                    rpc.call(Utils.UNSUBSCRIBE_AS_VIEWER, subscriberPacket.serialize());
                } catch (NumberFormatException ignored) {

                }
            }
        }
        visibleParticipants.set(visibleEmails);
    }

    private void initComponents() {
        rpc.subscribe(Utils.UPDATE_UI, (args) -> {
            final RImage rImage = RImage.deserialize(args);
            final int[][] image = rImage.getImage();
            final int height = image.length;
            final int width = image[0].length;

            final BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            for (int x = 0; x < height; x++) {
                for (int y = 0; y < width; y++) {
                    bufferedImage.setRGB(y, x, image[x][y]);
                }
            }
            final UIImage uiImage = new UIImage(bufferedImage, rImage.getIp(), (byte) 1);
            onImageReceived.accept(uiImage);
            final byte[] res = new byte[1];
            res[0] = uiImage.isSuccess();
            return res;
        });
    }
}
