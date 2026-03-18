package com.augmego.minecraft.avatar.client;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import net.minecraft.client.MinecraftClient;

public final class BrowserSessionManager {
    static final String DEFAULT_URL = "https://www.google.com";
    private static final int DEFAULT_WORLD_BROWSER_WIDTH = 1280;
    private static final int DEFAULT_WORLD_BROWSER_HEIGHT = 720;

    private static MCEFBrowser browser;

    private BrowserSessionManager() {
    }

    public static MCEFBrowser getBrowser() {
        return browser;
    }

    public static MCEFBrowser getOrCreateBrowser() {
        if (browser != null) {
            return browser;
        }
        if (!MCEF.isInitialized()) {
            return null;
        }

        browser = MCEF.createBrowser(DEFAULT_URL, true);
        browser.resize(DEFAULT_WORLD_BROWSER_WIDTH, DEFAULT_WORLD_BROWSER_HEIGHT);
        browser.setFocus(false);
        return browser;
    }

    public static void ensureBrowserSize(int width, int height) {
        MCEFBrowser currentBrowser = getOrCreateBrowser();
        if (currentBrowser == null) {
            return;
        }

        currentBrowser.resize(Math.max(1, width), Math.max(1, height));
    }

    public static void openFullscreen(MinecraftClient client) {
        if (client == null) {
            return;
        }

        getOrCreateBrowser();
        if (!(client.currentScreen instanceof InGameBrowserScreen)) {
            client.setScreen(new InGameBrowserScreen());
        }
    }
}
