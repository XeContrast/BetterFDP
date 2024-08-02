/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import me.liuli.elixir.account.MinecraftAccount;
import net.ccbluex.liquidbounce.FDPClient;
import net.ccbluex.liquidbounce.handler.network.AutoReconnect;
import net.ccbluex.liquidbounce.handler.network.ClientFixes;
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager;
import net.ccbluex.liquidbounce.utils.ServerUtils;
import net.ccbluex.liquidbounce.utils.SessionUtils;
import net.ccbluex.liquidbounce.utils.extensions.RendererExtensionKt;
import net.ccbluex.liquidbounce.utils.login.LoginUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

@Mixin(GuiDisconnected.class)
public abstract class MixinGuiDisconnected extends MixinGuiScreen {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0");

    @Shadow
    private int field_175353_i;

    @Shadow @Final private GuiScreen parentScreen;
    private GuiButton fDPClient$reconnectButton;
    private GuiSlider fDPClient$autoReconnectDelaySlider;
    private GuiButton fDPClient$forgeBypassButton;
    private int fDPClient$reconnectTimer;
    private String fDPClient$infoStr = "null";

    @Inject(method = "initGui", at = @At("RETURN"))
    private void initGui(CallbackInfo callbackInfo) {
        fDPClient$reconnectTimer = 0;
        SessionUtils.handleConnection();

        final ServerData server=ServerUtils.serverData;
        fDPClient$infoStr ="§fPlaying on: "+mc.session.getUsername()+" | "+server.serverIP;
        buttonList.add(fDPClient$reconnectButton = new GuiButton(1, this.width / 2 - 100, this.height / 2 + field_175353_i / 2 + this.fontRendererObj.FONT_HEIGHT + 22, 98, 20, "Reconnect"));

        buttonList.add(fDPClient$autoReconnectDelaySlider =
                new GuiSlider(2, this.width / 2 + 2, this.height / 2 + field_175353_i / 2
                        + this.fontRendererObj.FONT_HEIGHT + 22, 98, 20, "AutoReconnect: ",
                        "ms", AutoReconnect.MIN, AutoReconnect.MAX, AutoReconnect.INSTANCE.getDelay(), false, true,
                        guiSlider -> {
                            AutoReconnect.INSTANCE.setDelay(guiSlider.getValueInt());

                            this.fDPClient$reconnectTimer = 0;
                            this.fDPClient$updateReconnectButton();
                            this.fDPClient$updateSliderText();
                        }));

        buttonList.add(new GuiButton(3, this.width / 2 - 100, this.height / 2 + field_175353_i / 2 + this.fontRendererObj.FONT_HEIGHT + 44, 98, 20, "RandomAlt"));
        buttonList.add(new GuiButton(4, this.width / 2 + 2, this.height / 2 + field_175353_i / 2 + this.fontRendererObj.FONT_HEIGHT + 44, 98, 20, "RandomOffline"));
        buttonList.add(fDPClient$forgeBypassButton = new GuiButton(5, this.width / 2 - 100, this.height / 2 + field_175353_i / 2 + this.fontRendererObj.FONT_HEIGHT + 66, "AntiForge: "));

        fDPClient$updateSliderText();
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void actionPerformed(GuiButton button, CallbackInfo callbackInfo) {
        switch (button.id) {
            case 1:
                ServerUtils.connectToLastServer();
                break;
            case 3:
                final List<MinecraftAccount> accounts = FDPClient.fileManager.getAccountsConfig().getAltManagerMinecraftAccounts();
                if (accounts.isEmpty()) break;

                final MinecraftAccount minecraftAccount = accounts.get(new Random().nextInt(accounts.size()));
                GuiAltManager.Companion.login(minecraftAccount);
                ServerUtils.connectToLastServer();
                break;
            case 4:
                LoginUtils.INSTANCE.randomCracked();
                ServerUtils.connectToLastServer();
                break;
            case 5:
                ClientFixes.INSTANCE.setEnabled(!ClientFixes.INSTANCE.getEnabled());
                fDPClient$forgeBypassButton.displayString = "AntiForge: " + (ClientFixes.INSTANCE.getEnabled() ? "ON" : "OFF");
                FDPClient.fileManager.saveConfig(FDPClient.fileManager.getSpecialConfig());
                break;
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void drawScreen(CallbackInfo callbackInfo) {
        RendererExtensionKt.drawCenteredString(mc.fontRendererObj, fDPClient$infoStr, this.width / 2F, this.height / 2F + field_175353_i / 2F + this.fontRendererObj.FONT_HEIGHT + 100, 0, false);
        if (AutoReconnect.INSTANCE.isEnabled()) {
            this.fDPClient$updateReconnectButton();
        }
    }

    private void fDPClient$updateSliderText() {
        if (this.fDPClient$autoReconnectDelaySlider == null)
            return;

        if (!AutoReconnect.INSTANCE.isEnabled()) {
            this.fDPClient$autoReconnectDelaySlider.displayString = "AutoReconnect: Off";
        } else {
            this.fDPClient$autoReconnectDelaySlider.displayString = "AutoReconnect: " + DECIMAL_FORMAT.format(AutoReconnect.INSTANCE.getDelay() / 1000.0) + "s";
        }
    }

    private void fDPClient$updateReconnectButton() {
        if (fDPClient$reconnectButton != null)
            fDPClient$reconnectButton.displayString = "Reconnect" + (AutoReconnect.INSTANCE.isEnabled() ? " (" + (AutoReconnect.INSTANCE.getDelay() / 1000 - fDPClient$reconnectTimer / 20) + ")" : "");
    }

    @Inject(method = "keyTyped", at = @At("HEAD"))
    private void keyTyped(char typedChar, int keyCode, CallbackInfo callbackInfo) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parentScreen);
        }
    }
}