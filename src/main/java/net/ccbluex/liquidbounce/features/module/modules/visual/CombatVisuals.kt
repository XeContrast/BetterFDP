/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.FDPClient
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.Criticals
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.CombatRender.drawCrystal
import net.ccbluex.liquidbounce.utils.render.CombatRender.drawEntityBoxESP
import net.ccbluex.liquidbounce.utils.render.CombatRender.drawPlatformESP
import net.ccbluex.liquidbounce.utils.render.CombatRender.drawZavz
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawPlatform
import net.minecraft.block.Block
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.effect.EntityLightningBolt
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity
import net.minecraft.potion.Potion
import net.minecraft.util.EnumParticleTypes
import java.awt.Color
import java.util.*

@ModuleInfo("CombatVisuals", category = ModuleCategory.VISUAL)
object CombatVisuals : Module() {

    // Mark
    private val markValue = ListValue("MarkMode", arrayOf("None", "Box", "RoundBox", "Head", "Mark", "Sims", "Zavz"), "Zavz")
    private val isMarkMode: Boolean
        get() = markValue.get() != "None"

    val colorRedValue = IntegerValue("Mark-Red", 0, 0,255).displayable { isMarkMode }
    val colorGreenValue = IntegerValue("Mark-Green", 160, 0,255).displayable { isMarkMode }
    val colorBlueValue = IntegerValue("Mark-Blue", 255, 0,255).displayable { isMarkMode }

    private val alphaValue = IntegerValue("Alpha", 255, 0,255).displayable { isMarkMode && markValue.get() == "Zavz" }

    val colorRedTwoValue = IntegerValue("Mark-Red 2", 0, 0, 255).displayable { isMarkMode && markValue.get() == "Zavz" }
    val colorGreenTwoValue = IntegerValue("Mark-Green 2", 160, 0,255).displayable { isMarkMode && markValue.get() == "Zavz" }
    val colorBlueTwoValue = IntegerValue("Mark-Blue 2", 255, 0,255).displayable { isMarkMode && markValue.get() == "Zavz" }

    private val rainbow = BoolValue("Mark-RainBow", false).displayable { isMarkMode }
    private val hurt = BoolValue("Mark-HurtTime", true).displayable { isMarkMode }
    private val boxOutline = BoolValue("Mark-Outline", true).displayable { isMarkMode && markValue.get() == "RoundBox" }

    // fake sharp
    private val fakeSharp = BoolValue("FakeSharp", true)

    // Sound

    private val particle = ListValue("Particle",
        arrayOf("None", "Blood", "Lighting", "Fire", "Heart", "Water", "Smoke", "Magic", "Crits"), "Blood")

    private val amount = IntegerValue("ParticleAmount", 5, 1,20) { particle.get() != "None" }

    //Sound
    private val sound = ListValue("Sound", arrayOf("None", "Hit", "Explode", "Orb", "Pop", "Splash", "Lightning"), "Pop")

    private val volume = FloatValue("Volume", 1f, 0.1f, 5f).displayable { sound.get() != "None" }
    private val pitch = FloatValue("Pitch", 1f, 0.1f,5f).displayable { sound.get() != "None" }

    // variables
    private val targetList = HashMap<EntityLivingBase, Long>()
    private val combat = FDPClient.combatManager
    var random = Random()
    const val DOUBLE_PI = Math.PI * 2
    var start = 0.0

    @EventTarget
    fun onWorld(event: WorldEvent?) {
        targetList.clear()
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val color: Color = if (rainbow.get()) ColorUtils.rainbow() else Color(
            colorRedValue.get(),
            colorGreenValue.get(),
            colorBlueValue.get(),
            alphaValue.get()
        )
        val renderManager = mc.renderManager
        val entityLivingBase = combat.target ?: return
        (entityLivingBase.lastTickPosX + (entityLivingBase.posX - entityLivingBase.lastTickPosX) * mc.timer.renderPartialTicks
                - renderManager.renderPosX)
        (entityLivingBase.lastTickPosY + (entityLivingBase.posY - entityLivingBase.lastTickPosY) * mc.timer.renderPartialTicks
                - renderManager.renderPosY)
        (entityLivingBase.lastTickPosZ + (entityLivingBase.posZ - entityLivingBase.lastTickPosZ) * mc.timer.renderPartialTicks
                - renderManager.renderPosZ)
        when (markValue.get().lowercase()) {
            "box" -> drawEntityBoxESP(
                entityLivingBase,
                if ((hurt.get() && entityLivingBase.hurtTime > 3)) Color(255, 50, 50, 75) else color
            )

            "roundbox" -> drawEntityBox(
                entityLivingBase,
                if (hurt.get() && entityLivingBase.hurtTime > 3)
                    Color(37, 126, 255, 70)
                else
                    Color(255, 0, 0, 70),
                boxOutline.get()
            )

            "head" -> drawPlatformESP(
                entityLivingBase,
                if ((hurt.get() && entityLivingBase.hurtTime > 3)) Color(255, 50, 50, 75) else color
            )

            "mark" -> drawPlatform(
                entityLivingBase,
                if ((hurt.get() && entityLivingBase.hurtTime > 3)) Color(37, 126, 255, 70) else color
            )

            "sims" -> drawCrystal(
                entityLivingBase,
                if ((hurt.get() && entityLivingBase.hurtTime <= 0)) Color(80, 255, 80, 200).rgb else Color(255, 0, 0, 200).rgb,
                event
            )

            "zavz" -> drawZavz(
                entityLivingBase,
                event,
                dual = true, // or false based on your requirement
            )
        }
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        val target = event.targetEntity as? EntityLivingBase ?: return

        repeat(amount.get()) {
            doEffect(target)
        }

        doSound()
        attackEntity(target)
    }

    @EventTarget
    private fun attackEntity(entity: EntityLivingBase) {
        val thePlayer = mc.thePlayer

        // Extra critical effects
        repeat(3) {
            // Critical Effect
            if (thePlayer.fallDistance > 0F && !thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isPotionActive(
                    Potion.blindness
                ) && thePlayer.ridingEntity == null || Criticals.handleEvents() && Criticals.msTimer.hasTimePassed(
                    Criticals.delayValue.get().toLong()
                ) && !thePlayer.isInWater && !thePlayer.isInLava && !thePlayer.isInWeb) {
                thePlayer.onCriticalHit(entity)
            }

            // Enchant Effect
            if (EnchantmentHelper.getModifierForCreature(thePlayer.heldItem,
                    entity.creatureAttribute
                ) > 0f || fakeSharp.get()
            ) {
                thePlayer.onEnchantmentCritical(entity)
            }
        }
    }

    private fun doSound() {
        val player = mc.thePlayer

        when (sound.get()) {
            "Hit" -> player.playSound("random.bowhit", volume.get(), pitch.get())
            "Orb" -> player.playSound("random.orb", volume.get(), pitch.get())
            "Pop" -> player.playSound("random.pop", volume.get(), pitch.get())
            "Splash" -> player.playSound("random.splash", volume.get(), pitch.get())
            "Lightning" -> player.playSound("ambient.weather.thunder", volume.get(), pitch.get())
            "Explode" -> player.playSound("random.explode", volume.get(), pitch.get())
        }
    }

    private fun doEffect(target: EntityLivingBase) {
        when (particle.get()) {
            "Blood" -> spawnBloodParticle(EnumParticleTypes.BLOCK_CRACK, target)
            "Crits" -> spawnEffectParticle(EnumParticleTypes.CRIT, target)
            "Magic" -> spawnEffectParticle(EnumParticleTypes.CRIT_MAGIC, target)
            "Lighting" -> spawnLightning(target)
            "Smoke" -> spawnEffectParticle(EnumParticleTypes.SMOKE_NORMAL, target)
            "Water" -> spawnEffectParticle(EnumParticleTypes.WATER_DROP, target)
            "Heart" -> spawnEffectParticle(EnumParticleTypes.HEART, target)
            "Fire" -> spawnEffectParticle(EnumParticleTypes.LAVA, target)
        }
    }

    private fun spawnBloodParticle(particleType: EnumParticleTypes, target: EntityLivingBase) {
        mc.theWorld.spawnParticle(particleType,
            target.posX, target.posY + target.height - 0.75, target.posZ,
            0.0, 0.0, 0.0,
            Block.getStateId(Blocks.redstone_block.defaultState)
        )
    }

    private fun spawnEffectParticle(particleType: EnumParticleTypes, target: EntityLivingBase) {
        mc.effectRenderer.spawnEffectParticle(particleType.particleID,
            target.posX, target.posY, target.posZ,
            target.posX, target.posY, target.posZ
        )
    }

    private fun spawnLightning(target: EntityLivingBase) {
        mc.netHandler.handleSpawnGlobalEntity(S2CPacketSpawnGlobalEntity(
            EntityLightningBolt(mc.theWorld, target.posX, target.posY, target.posZ)
        ))
    }
}