package com.podoon.bleuarttool.global;

/**
 * Created by zhuohf1 on 2017/9/5.
 */

public class Constants {
    public final static boolean DEBUG = true;

    /** Base path for all messages between the handheld and wearable. */
    private static final String BASE_PATH =  "/nrftoolbox";

    /** Action sent from a wearable to disconnect from a device. It must have the profile name set as data. */
    public static final String ACTION_DISCONNECT = BASE_PATH + "/disconnect";

    /**
     * Constants for the UART profile.
     */
    public static final class UART {
        /** The profile name. */
        public static final String PROFILE = "uart";

        /** Base path for UART messages between the handheld and wearable. */
        private static final String PROFILE_PATH =  BASE_PATH + "/uart";

        /** An UART device is connected. */
        public static final String DEVICE_CONNECTED = PROFILE_PATH + "/connected";
        /** An UART device is disconnected. */
        public static final String DEVICE_DISCONNECTED = PROFILE_PATH + "/disconnected";
        /** An UART device is disconnected due to a link loss. */
        public static final String DEVICE_LINKLOSS = PROFILE_PATH + "/link_loss";
        /** Path used for syncing UART configurations. */
        public static final String CONFIGURATIONS = PROFILE_PATH + "/configurations";
        /** An action with a command was clicked. */
        public static final String COMMAND = PROFILE_PATH + "/command";

        public static final class Configuration {
            public static final String NAME = "name";
            public static final String COMMANDS = "commands";

            public static final class Command {
                public static final String ICON_ID = "icon_id";
                public static final String MESSAGE = "message";
                public static final String EOL = "eol";
            }
        }
    }
}
