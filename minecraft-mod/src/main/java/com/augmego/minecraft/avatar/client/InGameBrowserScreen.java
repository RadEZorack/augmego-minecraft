package com.augmego.minecraft.avatar.client;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandler;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public final class InGameBrowserScreen extends Screen {
    private static final int FRAME_MARGIN = 20;
    private static final int NAV_BAR_HEIGHT = 20;
    private static final int NAV_BAR_GAP = 6;
    private static final int NAV_BUTTON_WIDTH = 24;
    private static final int NAV_SPACING = 4;
    private static final int LOADING_BAR_HEIGHT = 2;
    private static final int LOADING_BAR_TRACK_COLOR = 0x55000000;
    private static final int LOADING_BAR_FILL_COLOR = 0xFF3BA8FF;
    private MCEFBrowser browser;
    private TextFieldWidget urlField;
    private ButtonWidget backButton;
    private ButtonWidget forwardButton;
    private ButtonWidget reloadButton;
    private CefDisplayHandler addressBarDisplayHandler;

    public InGameBrowserScreen() {
        super(Text.literal("Augmego Browser"));
    }

    @Override
    protected void init() {
        super.init();
        ensureBrowserCreated();
        registerAddressBarDisplayHandler();
        initNavigationWidgets();
        resizeBrowser();
        refreshNavigationState();
    }

    private void ensureBrowserCreated() {
        browser = BrowserSessionManager.getOrCreateBrowser();
        if (browser != null) {
            browser.setFocus(true);
        }
    }

    private void registerAddressBarDisplayHandler() {
        if (addressBarDisplayHandler != null || !MCEF.isInitialized()) {
            return;
        }

        addressBarDisplayHandler = new CefDisplayHandlerAdapter() {
            @Override
            public void onAddressChange(CefBrowser cefBrowser, CefFrame frame, String url) {
                if (browser == null || cefBrowser == null || frame == null || !frame.isMain()) {
                    return;
                }
                if (cefBrowser.getIdentifier() != browser.getIdentifier()) {
                    return;
                }

                client.execute(() -> {
                    if (client.currentScreen != InGameBrowserScreen.this || urlField == null || url == null || url.isBlank()) {
                        return;
                    }
                    if (!url.equals(urlField.getText())) {
                        urlField.setText(url);
                    }
                });
            }
        };
        MCEF.getClient().addDisplayHandler(addressBarDisplayHandler);
    }

    private void initNavigationWidgets() {
        clearChildren();

        int navX = FRAME_MARGIN;
        int navY = FRAME_MARGIN;

        backButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("<"), button -> {
                if (browser != null) {
                    browser.goBack();
                }
            }).dimensions(navX, navY, NAV_BUTTON_WIDTH, NAV_BAR_HEIGHT).build()
        );
        navX += NAV_BUTTON_WIDTH + NAV_SPACING;

        forwardButton = addDrawableChild(
            ButtonWidget.builder(Text.literal(">"), button -> {
                if (browser != null) {
                    browser.goForward();
                }
            }).dimensions(navX, navY, NAV_BUTTON_WIDTH, NAV_BAR_HEIGHT).build()
        );
        navX += NAV_BUTTON_WIDTH + NAV_SPACING;

        reloadButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("R"), button -> {
                if (browser != null) {
                    browser.reload();
                }
            }).dimensions(navX, navY, NAV_BUTTON_WIDTH, NAV_BAR_HEIGHT).build()
        );
        navX += NAV_BUTTON_WIDTH + NAV_SPACING;

        int urlWidth = Math.max(60, width - FRAME_MARGIN - navX);
        urlField = addDrawableChild(new TextFieldWidget(textRenderer, navX, navY, urlWidth, NAV_BAR_HEIGHT, Text.literal("URL")));
        urlField.setMaxLength(2048);
        urlField.setText(
            browser == null || browser.getURL() == null || browser.getURL().isBlank()
                ? BrowserSessionManager.DEFAULT_URL
                : browser.getURL()
        );
    }

    private int getBrowserX() {
        return FRAME_MARGIN;
    }

    private int getBrowserY() {
        return FRAME_MARGIN + NAV_BAR_HEIGHT + NAV_BAR_GAP;
    }

    private int getBrowserWidth() {
        return Math.max(1, width - FRAME_MARGIN * 2);
    }

    private int getBrowserHeight() {
        return Math.max(1, height - getBrowserY() - FRAME_MARGIN);
    }

    private boolean isInBrowserBounds(double x, double y) {
        int browserX = getBrowserX();
        int browserY = getBrowserY();
        return x >= browserX && y >= browserY && x < browserX + getBrowserWidth() && y < browserY + getBrowserHeight();
    }

    private int browserMouseX(double x) {
        return (int) ((x - getBrowserX()) * client.getWindow().getScaleFactor());
    }

    private int browserMouseY(double y) {
        return (int) ((y - getBrowserY()) * client.getWindow().getScaleFactor());
    }

    private void resizeBrowser() {
        if (browser == null || client == null) {
            return;
        }

        browser.resize(
            (int) (getBrowserWidth() * client.getWindow().getScaleFactor()),
            (int) (getBrowserHeight() * client.getWindow().getScaleFactor())
        );
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        resizeBrowser();
    }

    @Override
    public void removed() {
        if (addressBarDisplayHandler != null && MCEF.isInitialized()) {
            MCEF.getClient().removeDisplayHandler(addressBarDisplayHandler);
        }
        addressBarDisplayHandler = null;
        if (browser != null) {
            browser.setFocus(false);
        }
        super.removed();
    }

    @Override
    public void tick() {
        super.tick();
        if (browser == null) {
            ensureBrowserCreated();
            registerAddressBarDisplayHandler();
            resizeBrowser();
        }
        refreshNavigationState();
    }

    private void refreshNavigationState() {
        if (backButton != null) {
            backButton.active = browser != null && browser.canGoBack();
        }
        if (forwardButton != null) {
            forwardButton.active = browser != null && browser.canGoForward();
        }
        if (reloadButton != null) {
            reloadButton.active = browser != null;
        }

        if (browser != null && urlField != null && !urlField.isFocused()) {
            String currentUrl = browser.getURL();
            if (currentUrl != null && !currentUrl.isBlank() && !currentUrl.equals(urlField.getText())) {
                urlField.setText(currentUrl);
            }
        }
    }

    private void navigateFromUrlField() {
        if (urlField == null || browser == null) {
            return;
        }

        String input = urlField.getText();
        if (input == null) {
            return;
        }

        input = input.trim();
        if (input.isEmpty()) {
            return;
        }

        String normalizedUrl = normalizeUrl(input);
        urlField.setText(normalizedUrl);
        browser.loadURL(normalizedUrl);
        browser.setFocus(true);
    }

    private static String normalizeUrl(String input) {
        if (input.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
            return input;
        }
        return "https://" + input;
    }

    @Override
    public void render(@NotNull DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderInGameBackground(context);
        super.render(context, mouseX, mouseY, deltaTicks);

        renderLoadingIndicator(context);
        if (browser != null && browser.isTextureReady()) {
            renderBrowserTexture(context);
        } else {
            context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal(MCEF.isInitialized() ? "Waiting for browser texture..." : "Waiting for MCEF to initialize..."),
                width / 2,
                getBrowserY() + 12,
                0xFFFFFFFF
            );
        }
    }

    private void renderBrowserTexture(DrawContext context) {
        if (browser == null || browser.getTextureIdentifier() == null) {
            return;
        }

        int browserWidth = getBrowserWidth();
        int browserHeight = getBrowserHeight();
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            browser.getTextureIdentifier(),
            getBrowserX(),
            getBrowserY(),
            0.0F,
            0.0F,
            browserWidth,
            browserHeight,
            browserWidth,
            browserHeight
        );
    }

    private void renderLoadingIndicator(DrawContext context) {
        if (browser == null || urlField == null || !browser.isLoading()) {
            return;
        }

        int barX = urlField.getX();
        int barY = urlField.getY() + 1;
        int barWidth = urlField.getWidth();
        int barBottom = barY + LOADING_BAR_HEIGHT;
        context.fill(barX, barY, barX + barWidth, barBottom, LOADING_BAR_TRACK_COLOR);

        int segmentWidth = Math.max(20, barWidth / 4);
        int travelRange = barWidth + segmentWidth;
        int animatedOffset = (int) ((UtilShim.getMeasuringTimeMs() / 6L) % travelRange) - segmentWidth;
        int segmentStart = Math.max(barX, barX + animatedOffset);
        int segmentEnd = Math.min(barX + barWidth, barX + animatedOffset + segmentWidth);
        if (segmentEnd > segmentStart) {
            context.fill(segmentStart, barY, segmentEnd, barBottom, LOADING_BAR_FILL_COLOR);
        }
    }

    @Override
    public boolean mouseClicked(Click event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        if (handled || browser == null) {
            return handled;
        }

        if (!isInBrowserBounds(event.x(), event.y())) {
            return false;
        }

        browser.sendMousePress(browserMouseX(event.x()), browserMouseY(event.y()), event.button());
        browser.setFocus(true);
        return true;
    }

    @Override
    public boolean mouseReleased(Click event) {
        boolean handled = super.mouseReleased(event);
        if (handled || browser == null) {
            return handled;
        }

        browser.sendMouseRelease(browserMouseX(event.x()), browserMouseY(event.y()), event.button());
        browser.setFocus(true);
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (browser != null && isInBrowserBounds(mouseX, mouseY)) {
            browser.sendMouseMove(browserMouseX(mouseX), browserMouseY(mouseY));
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        boolean handled = super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        if (handled || browser == null) {
            return handled;
        }

        if (!isInBrowserBounds(mouseX, mouseY)) {
            return false;
        }

        browser.sendMouseWheel(browserMouseX(mouseX), browserMouseY(mouseY), verticalAmount, 0);
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput event) {
        if (urlField != null && urlField.isFocused()
            && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            navigateFromUrlField();
            setFocused(null);
            if (browser != null) {
                browser.setFocus(true);
            }
            return true;
        }

        if (super.keyPressed(event)) {
            return true;
        }

        if (browser == null || (urlField != null && urlField.isFocused())) {
            return false;
        }

        browser.sendKeyPress(event.key(), event.scancode(), event.modifiers());
        browser.setFocus(true);
        return true;
    }

    @Override
    public boolean keyReleased(net.minecraft.client.input.KeyInput event) {
        if (super.keyReleased(event)) {
            return true;
        }

        if (browser == null || (urlField != null && urlField.isFocused())) {
            return false;
        }

        browser.sendKeyRelease(event.key(), event.scancode(), event.modifiers());
        browser.setFocus(true);
        return true;
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput event) {
        if (super.charTyped(event)) {
            return true;
        }

        if (browser == null || (urlField != null && urlField.isFocused()) || event.codepoint() == 0) {
            return false;
        }

        browser.sendKeyTyped((char) event.codepoint(), event.modifiers());
        browser.setFocus(true);
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static final class UtilShim {
        private UtilShim() {
        }

        private static long getMeasuringTimeMs() {
            return net.minecraft.util.Util.getMeasuringTimeMs();
        }
    }
}
