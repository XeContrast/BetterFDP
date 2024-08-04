/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import cn.hanabi.gui.cloudmusic.ui.MusicOverlayRenderer
import net.ccbluex.liquidbounce.FDPClient
import net.ccbluex.liquidbounce.FDPClient.CLIENT_NAME
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.font.FontLoaders
import net.ccbluex.liquidbounce.ui.cape.GuiCapeManager.height
import net.ccbluex.liquidbounce.ui.client.gui.ClickGUIModule.colorBlueValue
import net.ccbluex.liquidbounce.ui.client.gui.ClickGUIModule.colorGreenValue
import net.ccbluex.liquidbounce.ui.client.gui.ClickGUIModule.colorRedValue
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.MathHelper
import net.minecraft.util.ResourceLocation
import java.awt.Color
import java.util.*

@ModuleInfo(name = "HUD", category = ModuleCategory.CLIENT, array = false, defaultOn = true)
object HUD : Module() {
    val tabHead = BoolValue("Tab-HeadOverlay", true)
    val shadowValue = ListValue("TextShadowMode", arrayOf("LiquidBounce", "Outline", "Default", "Autumn"), "Default")
    private val clolormode = ListValue("ColorMode", arrayOf("Rainbow", "Light Rainbow", "Static", "Double Color", "Default"), "Light Rainbow")
    private val MusicDisplay = BoolValue("MusicDisplay",true)
    val inventoryOnHotbar = BoolValue("InventoryOnHotbar", true)
    val hueInterpolation = BoolValue("HueInterpolation", false)
    val movingcolors = BoolValue("MovingColors", false)
    val inventoryParticle = BoolValue("InventoryParticle", false)
    private val blurValue = BoolValue("Blur", false)
    private val HealthValue = BoolValue("Health", true)
    private val mark = ListValue("Mark", arrayOf("FDPCN","FDP","None"),"FDP")
    val rainbowStartValue = FloatValue("RainbowStart", 0.55f, 0f, 1f)
    val rainbowStopValue = FloatValue("RainbowStop", 0.85f, 0f, 1f)
    val rainbowSaturationValue = FloatValue("RainbowSaturation", 0.45f, 0f, 1f)
    val rainbowBrightnessValue = FloatValue("RainbowBrightness", 0.85f, 0f, 1f)
    val rainbowSpeedValue = IntegerValue("RainbowSpeed", 1500, 500, 7000)
    val arraylistXAxisAnimSpeedValue = IntegerValue("ArraylistXAxisAnimSpeed", 10, 5, 20)
    val arraylistXAxisAnimTypeValue = EaseUtils.getEnumEasingList("ArraylistXAxisAnimType")
    val arraylistXAxisAnimOrderValue = EaseUtils.getEnumEasingOrderList("ArraylistXAxisHotbarAnimOrder").displayable { !arraylistXAxisAnimTypeValue.equals("NONE") }
    val arraylistYAxisAnimSpeedValue = IntegerValue("ArraylistYAxisAnimSpeed", 10, 5, 20)
    val arraylistYAxisAnimTypeValue = EaseUtils.getEnumEasingList("ArraylistYAxisAnimType")
    val arraylistYAxisAnimOrderValue = EaseUtils.getEnumEasingOrderList("ArraylistYAxisHotbarAnimOrder").displayable { !arraylistYAxisAnimTypeValue.equals("NONE") }
    private val fontEpsilonValue = FloatValue("FontVectorEpsilon", 0.5f, 0f, 1.5f)

    private var lastFontEpsilon = 0f

    /**
     * Renders the HUD.
     */
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (MusicDisplay.get()) MusicOverlayRenderer.INSTANCE.renderOverlay()
        if (mc.currentScreen is GuiHudDesigner) return
        FDPClient.hud.render(false, event.partialTicks)
        if (mark.equals("FDP")) renderWatermark()
        if (mark.equals("FDPCN")) renderfdpcn()
        if (HealthValue.get()) mc.fontRendererObj.drawStringWithShadow(
            MathHelper.ceiling_float_int(mc.thePlayer.health).toString(),
            (width / 2 - 4).toFloat(), (height / 2 - 13).toFloat(), if (mc.thePlayer.health <= 15) Color(255, 0, 0).rgb else Color(0, 255, 0).rgb)
        GlStateManager.resetColor()
    }

    /**
     * Renders the watermark.
     */
    private fun renderWatermark() {
        var width = 3
        val colors = getClientColors()
        mc.fontRendererObj.drawStringWithShadow(
            "FDP",
            3.0f,
            3.0f,
            colors[0].rgb
        )
        width += mc.fontRendererObj.getStringWidth("FDP")
        mc.fontRendererObj.drawStringWithShadow(
            "CLIENT",
            width.toFloat(),
            3.0f,
            colors[1].rgb
        )
    }
    private fun getClientName(i: Int, i2: Int): String{
        return "FDPClient".substring(i,i2)
    }
    private fun renderfdpcn() {
        FontLoaders.F40.drawString(

            getClientName(0,3), 5.0f, 0.0f,Color(255,255,255,220).rgb
        )
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        FDPClient.hud.update()
        if (mc.currentScreen == null && lastFontEpsilon != fontEpsilonValue.get()) {
            lastFontEpsilon = fontEpsilonValue.get()
            alert("You need to reload FDPClient to apply changes!")
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        lastFontEpsilon = fontEpsilonValue.get()
    }

    @EventTarget(ignoreCondition = true)
    fun onScreen(event: ScreenEvent) {
        if (mc.theWorld == null || mc.thePlayer == null) return
        if (state && blurValue.get() && !mc.entityRenderer.isShaderActive && event.guiScreen != null &&
            !(event.guiScreen is GuiChat || event.guiScreen is GuiHudDesigner)) mc.entityRenderer.loadShader(
            ResourceLocation(CLIENT_NAME.lowercase() + "/blur.json")
        ) else if (mc.entityRenderer.shaderGroup != null &&
            "fdpclient/blur.json" in mc.entityRenderer.shaderGroup.shaderGroupName) mc.entityRenderer.stopUseShader()
    }

    @EventTarget
    fun onKey(event: KeyEvent) {
        FDPClient.hud.handleKey('a', event.key)
    }

    fun getClientColors(): Array<Color> {
        val firstColor: Color
        val secondColor: Color
        when (clolormode.get().lowercase(Locale.getDefault())) {
            "light rainbow" -> {
                firstColor = ColorUtils.rainbowc(15, 1, .6f, 1F, 1F)!!
                secondColor = ColorUtils.rainbowc(15, 40, .6f, 1F, 1F)!!
            }
            "rainbow" -> {
                firstColor = ColorUtils.rainbowc(15, 1, 1F, 1F, 1F)!!
                secondColor = ColorUtils.rainbowc(15, 40, 1F, 1F, 1F)!!
            }
            "double color" -> {
                firstColor =
                    ColorUtils.interpolateColorsBackAndForth(15, 0, Color.PINK, Color.BLUE, hueInterpolation.get())!!
                secondColor =
                    ColorUtils.interpolateColorsBackAndForth(15, 90, Color.PINK, Color.BLUE, hueInterpolation.get())!!
            }
            "static" -> {
                firstColor = Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())
                secondColor = firstColor
            }
            else -> {
                firstColor = Color(-1)
                secondColor = Color(-1)
            }
        }
        return arrayOf(firstColor, secondColor)
    }

}
