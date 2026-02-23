package tw.nekomimi.nekogram;

import org.telegram.messenger.BuildConfig;

import tw.nekomimi.nekogram.helpers.UserHelper;

public class Extra {
    // https://core.telegram.org/api/obtaining_api_id
    public static final int APP_ID = 25021578;
    public static final String APP_HASH = "dd92e0a3f0aa00f49ea0c26f441587b9";

    public static final String PLAYSTORE_APP_URL = "";

    public static String WS_USER_AGENT = "";
    public static String WS_CONN_HASH = "";
    public static String WS_DEFAULT_DOMAIN = "";

    public static String TWPIC_BOT_USERNAME = null;

    public static boolean FORCE_ANALYTICS = false;

    public static String TLV_URL = "";

    public static String SENTRY_DSN = "";

    public static boolean isDirectApp() {
        return "release".equals(BuildConfig.BUILD_TYPE) || "debug".equals(BuildConfig.BUILD_TYPE);
    }

    public static UserHelper.BotInfo getHelperBot() {
        return null;
    }

    public static UserHelper.UserInfoBot getUserInfoBot(boolean fallback) {
        return null;
    }

    public static boolean isTrustedBot(long id) {
        return false;
    }
}