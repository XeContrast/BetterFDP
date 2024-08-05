/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.FDPClient
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.Timer
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemEnderPearl
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.world.WorldSettings
import java.lang.Math.toRadians
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@ModuleInfo(name = "AntiVoid", category = ModuleCategory.PLAYER)
object AntiVoid : Module() {

    private val modeValue = ListValue(
        "Mode",
        arrayOf(
            "Blink",
            "TPBack",
            "MotionFlag",
            "PacketFlag",
            "GroundSpoof",
            "OldHypixel",
            "Jartex",
            "OldCubecraft",
            "Packet",
            "Vulcan",
            "SearchPearl"
        ),
        "Blink"
    )
    private val maxFallDistValue = FloatValue("MaxFallDistance", 10F, 5F, 20F)
    private val freezeValue = ListValue("FreeZeMode", arrayOf("Cancel","LowTimer"),"Cancel").displayable{ modeValue.equals("SearchPearl")}
    private val resetMotionValue = BoolValue("ResetMotion", false).displayable { modeValue.equals("Blink") }
    private val startFallDistValue =
        FloatValue("BlinkStartFallDistance", 2F, 0F, 5F).displayable { modeValue.equals("Blink") }
    private val autoScaffoldValue = BoolValue("BlinkAutoScaffold", true).displayable { modeValue.equals("Blink") }
    private val motionflagValue =
        FloatValue("MotionFlag-MotionY", 1.0F, 0.0F, 5.0F).displayable { modeValue.equals("MotionFlag") }
    private val voidOnlyValue = BoolValue("OnlyVoid", true)

    private val packetCache = ArrayList<C03PacketPlayer>()
    private var blink = false
    private var canBlink = false
    private var canCancel = false
    private var canSpoof = false
    private var tried = false
    private var flagged = false

    private var posX = 0.0
    private var posY = 0.0
    private var posZ = 0.0
    private var motionX = 0.0
    private var motionY = 0.0
    private var motionZ = 0.0
    private var lastRecY = 0.0
    private var x = 0.0
    private var y = 0.0
    private var z = 0.0
    private var freeze = false
    private var forceRotateDetected = false
    private var ghostBlockDetected = false
    private var lastYaw = 0F
    private var lastPitch = 0F
    private var lagbackDetected = false
    private var fall = 0F
    private var lastPositon = intArrayOf(0, 0, 0)
    private var ticks = 0

    override fun onEnable() {
        fall = 0f
        forceRotateDetected = false
        ghostBlockDetected = false
        lagbackDetected = false
        freeze = false
        canCancel = false
        blink = false
        canBlink = false
        canSpoof = false
        lastRecY = if (mc.thePlayer != null) {
            mc.thePlayer.posY
        } else {
            0.0
        }
        tried = false
        flagged = false
        ticks = 0
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        if (lastRecY == 0.0) {
            lastRecY = mc.thePlayer.posY
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.onGround) {
            tried = false
            flagged = false
        }

        when (modeValue.get().lowercase()) {
            "groundspoof" -> {
                if (!voidOnlyValue.get() || checkVoid()) {
                    canSpoof = mc.thePlayer.fallDistance > maxFallDistValue.get()
                }
            }

            "searchpearl" -> {
                if (mc.currentScreen != null || mc.playerController.currentGameType == WorldSettings.GameType.SPECTATOR
                    || mc.playerController.currentGameType == WorldSettings.GameType.CREATIVE) return
                val entity = getClosestEntity()
                val distance = if (entity != null) mc.thePlayer.getDistanceToEntity(entity) else 0F
                freeze = distance > 4 || entity == null

                val pearl = InventoryUtils.findItem(36, 45, Items.ender_pearl)
                if (mc.thePlayer.fallDistance > maxFallDistValue.get() && (pearl - 36) > -1) {
                    if (!voidOnlyValue.get() || checkVoid()) {
                        mc.thePlayer.inventory.currentItem = pearl - 36
                    }
                }
            }

            "vulcan" -> {
                if (mc.thePlayer.onGround && BlockUtils.getBlock(
                        BlockPos(
                            mc.thePlayer.posX,
                            mc.thePlayer.posY - 1.0,
                            mc.thePlayer.posZ
                        )
                    ) !is BlockAir
                ) {
                    posX = mc.thePlayer.prevPosX
                    posY = mc.thePlayer.prevPosY
                    posZ = mc.thePlayer.prevPosZ
                }
                if (!voidOnlyValue.get() || checkVoid()) {
                    if (mc.thePlayer.fallDistance > maxFallDistValue.get() && !tried) {
                        mc.thePlayer.setPosition(mc.thePlayer.posX, posY, mc.thePlayer.posZ)
                        mc.netHandler.addToSendQueue(
                            C03PacketPlayer.C04PacketPlayerPosition(
                                mc.thePlayer.posX,
                                mc.thePlayer.posY,
                                mc.thePlayer.posZ,
                                false
                            )
                        )
                        mc.thePlayer.setPosition(posX, posY, posZ)
                        mc.netHandler.addToSendQueue(
                            C03PacketPlayer.C04PacketPlayerPosition(
                                mc.thePlayer.posX,
                                mc.thePlayer.posY,
                                mc.thePlayer.posZ,
                                true
                            )
                        )
                        mc.thePlayer.fallDistance = 0F
                        MovementUtils.resetMotion(true)
                        tried = true
                    }
                }
            }

            "motionflag" -> {
                if (!voidOnlyValue.get() || checkVoid()) {
                    if (mc.thePlayer.fallDistance > maxFallDistValue.get() && !tried) {
                        mc.thePlayer.motionY += motionflagValue.get()
                        mc.thePlayer.fallDistance = 0.0F
                        tried = true
                    }
                }
            }

            "packetflag" -> {
                if (!voidOnlyValue.get() || checkVoid()) {
                    if (mc.thePlayer.fallDistance > maxFallDistValue.get() && !tried) {
                        mc.netHandler.addToSendQueue(
                            C03PacketPlayer.C04PacketPlayerPosition(
                                mc.thePlayer.posX + 1,
                                mc.thePlayer.posY + 1,
                                mc.thePlayer.posZ + 1,
                                false
                            )
                        )
                        tried = true
                    }
                }
            }

            "tpback" -> {
                if (mc.thePlayer.onGround && BlockUtils.getBlock(
                        BlockPos(
                            mc.thePlayer.posX,
                            mc.thePlayer.posY - 1.0,
                            mc.thePlayer.posZ
                        )
                    ) !is BlockAir
                ) {
                    posX = mc.thePlayer.prevPosX
                    posY = mc.thePlayer.prevPosY
                    posZ = mc.thePlayer.prevPosZ
                }
                if (!voidOnlyValue.get() || checkVoid()) {
                    if (mc.thePlayer.fallDistance > maxFallDistValue.get() && !tried) {
                        mc.thePlayer.setPositionAndUpdate(posX, posY, posZ)
                        mc.thePlayer.fallDistance = 0F
                        mc.thePlayer.motionX = 0.0
                        mc.thePlayer.motionY = 0.0
                        mc.thePlayer.motionZ = 0.0
                        tried = true
                    }
                }
            }

            "jartex" -> {
                canSpoof = false
                if (!voidOnlyValue.get() || checkVoid()) {
                    if (mc.thePlayer.fallDistance > maxFallDistValue.get() && mc.thePlayer.posY < lastRecY + 0.01 && mc.thePlayer.motionY <= 0 && !mc.thePlayer.onGround && !flagged) {
                        mc.thePlayer.motionY = 0.0
                        mc.thePlayer.motionZ *= 0.838
                        mc.thePlayer.motionX *= 0.838
                        canSpoof = true
                    }
                }
                lastRecY = mc.thePlayer.posY
            }

            "oldcubecraft" -> {
                canSpoof = false
                if (!voidOnlyValue.get() || checkVoid()) {
                    if (mc.thePlayer.fallDistance > maxFallDistValue.get() && mc.thePlayer.posY < lastRecY + 0.01 && mc.thePlayer.motionY <= 0 && !mc.thePlayer.onGround && !flagged) {
                        mc.thePlayer.motionY = 0.0
                        mc.thePlayer.motionZ = 0.0
                        mc.thePlayer.motionX = 0.0
                        mc.thePlayer.jumpMovementFactor = 0.00f
                        canSpoof = true
                        if (!tried) {
                            tried = true
                            mc.netHandler.addToSendQueue(
                                C03PacketPlayer.C04PacketPlayerPosition(
                                    mc.thePlayer.posX,
                                    (32000.0).toDouble(),
                                    mc.thePlayer.posZ,
                                    false
                                )
                            )
                        }
                    }
                }
                lastRecY = mc.thePlayer.posY
            }

            "packet" -> {
                if (checkVoid()) {
                    canCancel = true
                }

                if (canCancel) {
                    if (mc.thePlayer.onGround) {
                        for (packet in packetCache) {
                            mc.netHandler.addToSendQueue(packet)
                        }
                        packetCache.clear()
                    }
                    canCancel = false
                }
            }

            "blink" -> {
                if (!blink) {
                    val collide = FallingPlayer(mc.thePlayer).findCollision(60)
                    if (canBlink && (collide == null || (mc.thePlayer.posY - collide.y) > startFallDistValue.get())) {
                        posX = mc.thePlayer.posX
                        posY = mc.thePlayer.posY
                        posZ = mc.thePlayer.posZ
                        motionX = mc.thePlayer.motionX
                        motionY = mc.thePlayer.motionY
                        motionZ = mc.thePlayer.motionZ

                        packetCache.clear()
                        blink = true
                    }

                    if (mc.thePlayer.onGround) {
                        canBlink = true
                    }
                } else {
                    if (mc.thePlayer.fallDistance > maxFallDistValue.get()) {
                        mc.thePlayer.setPositionAndUpdate(posX, posY, posZ)
                        if (resetMotionValue.get()) {
                            mc.thePlayer.motionX = 0.0
                            mc.thePlayer.motionY = 0.0
                            mc.thePlayer.motionZ = 0.0
                            mc.thePlayer.jumpMovementFactor = 0.00f
                        } else {
                            mc.thePlayer.motionX = motionX
                            mc.thePlayer.motionY = motionY
                            mc.thePlayer.motionZ = motionZ
                            mc.thePlayer.jumpMovementFactor = 0.00f
                        }

                        if (autoScaffoldValue.get()) {
                            FDPClient.moduleManager[Scaffold::class.java]!!.state = true
                        }

                        packetCache.clear()
                        blink = false
                        canBlink = false
                    } else if (mc.thePlayer.onGround) {
                        blink = false

                        for (packet in packetCache) {
                            mc.netHandler.addToSendQueue(packet)
                        }
                    }
                }
            }
        }
    }

    private fun checkVoid(): Boolean {
        return (FallingPlayer(mc.thePlayer).findCollision(60) == null)
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (modeValue.equals("searchpearl")) {
            if (mc.currentScreen != null || mc.playerController.currentGameType == WorldSettings.GameType.SPECTATOR
                || mc.playerController.currentGameType == WorldSettings.GameType.CREATIVE
            ) return
            if (!voidOnlyValue.get() || checkVoid()) {
                if (mc.thePlayer.fallDistance > maxFallDistValue.get()) {
                    if (mc.thePlayer.heldItem!!.item is ItemEnderPearl) {
                        if (freezeValue.equals("Cancel")) {
                            event.cancelEvent()
                        } else {
                            ticks = 0
                            mc.timer.timerSpeed = 0.1f
                        }
                    }
                }
            } else {
                if (!freezeValue.equals("Cancel")) {
                        ticks ++
                    if (ticks == 1) {
                        mc.timer.timerSpeed = 1f
                    }
                }
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        when (modeValue.get().lowercase()) {
            "blink" -> {
                if (blink && (packet is C03PacketPlayer)) {
                    packetCache.add(packet)
                    event.cancelEvent()
                }
            }

            "searchpearl" -> {
                if (mc.currentScreen != null || mc.playerController.currentGameType == WorldSettings.GameType.SPECTATOR
                    || mc.playerController.currentGameType == WorldSettings.GameType.CREATIVE) return
                if (!voidOnlyValue.get() || checkVoid()) {
                    if (mc.thePlayer.fallDistance > maxFallDistValue.get()) {
                        if (freezeValue.equals("Cancel")) {
                            if (mc.thePlayer.heldItem!!.item is ItemEnderPearl) {
                                if (packet is C03PacketPlayer && packet !is C05PacketPlayerLook) {
                                    event.cancelEvent()
                                }
                                if (event.packet is S08PacketPlayerPosLook) {
                                    x = event.packet.x
                                    y = event.packet.y
                                    z = event.packet.z
                                    motionX = 0.0
                                    motionY = 0.0
                                    motionZ = 0.0
                                }
                            }
                        }
                    }
                }
            }

            "packet" -> {
                if (canCancel && (packet is C03PacketPlayer)) {
                    packetCache.add(packet)
                    event.cancelEvent()
                }

                if (packet is S08PacketPlayerPosLook) {
                    packetCache.clear()
                    canCancel = false
                }
            }

            "groundspoof" -> {
                if (canSpoof && (packet is C03PacketPlayer)) {
                    packet.onGround = true
                }
            }

            "jartex" -> {
                if (canSpoof && (packet is C03PacketPlayer)) {
                    packet.onGround = true
                }
                if (canSpoof && (packet is S08PacketPlayerPosLook)) {
                    flagged = true
                }
            }

            "oldcubecraft" -> {
                if (canSpoof && (packet is C03PacketPlayer)) {
                    if (packet.y < 1145.141919810) event.cancelEvent()
                }
                if (canSpoof && (packet is S08PacketPlayerPosLook)) {
                    flagged = true
                }
            }

            "oldhypixel" -> {
                if (packet is S08PacketPlayerPosLook && mc.thePlayer.fallDistance > 3.125) mc.thePlayer.fallDistance =
                    3.125f

                if (packet is C03PacketPlayer) {
                    if (voidOnlyValue.get() && mc.thePlayer.fallDistance >= maxFallDistValue.get() && mc.thePlayer.motionY <= 0 && checkVoid()) {
                        packet.y += 11.0
                    }
                    if (!voidOnlyValue.get() && mc.thePlayer.fallDistance >= maxFallDistValue.get()) packet.y += 11.0
                }
            }
        }
    }

    private fun getClosestEntity(): Entity? {
        val filteredEntities = mutableListOf<Entity>()
        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityPlayer && entity !== mc.thePlayer) {
                filteredEntities.add(entity)
            }
        }
        filteredEntities.sortWith(
            compareBy(
                { mc.thePlayer.getDistanceToEntity(it) },
                { mc.thePlayer.getDistanceToEntity(it) })
        )
        return filteredEntities.lastOrNull()
    }

    private fun calculateAngleDelta(newAngle: Float, oldAngle: Float): Float {
        var delta = newAngle - oldAngle
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360
        return abs(delta)
    }
    private fun detectGroundCollision(): Boolean {
        val blockPos = BlockPos(x, y, z)
        val blockState = mc.theWorld.getBlockState(blockPos)
        val block = blockState.block
        return isSolidBlock(block)
    }
    private fun isSolidBlock(block: Block): Boolean {
        val nonSolidBlocks = listOf(
            Blocks.air,
            Blocks.water,
            Blocks.flowing_water,
            Blocks.lava,
            Blocks.flowing_lava
            //...
        )
        return block !in nonSolidBlocks
    }
    private fun calculateEnderPearlLandingSpot(yaw: Float, pitch: Float): Vec3? {
        val initialPosition = Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ)
        val yawRad = toRadians(yaw.toDouble())
        val pitchRad = toRadians(pitch.toDouble())

        val cosPitchRad = cos(pitchRad)
        val sinYawRad = sin(yawRad)
        val cosYawRad = cos(yawRad)
        val sinPitchRad = sin(pitchRad)

        val motionX = -cosPitchRad * sinYawRad * 1
        val motionY = -sinPitchRad * 1
        val motionZ = cosPitchRad * cosYawRad * 1

        var motion = Vec3(motionX, motionY, motionZ)

        while (initialPosition.yCoord > 0) {
            motion = Vec3(motion.xCoord, motion.yCoord - 0.03 * 0.1, motion.zCoord)

            if (detectGroundCollision()) {
                return initialPosition
            }
        }
        return null
    }
    private fun foundYaw(): Double {
        val position = Vec3(lastPositon[0].toDouble(), lastPositon[1].toDouble(),
            lastPositon[2].toDouble()
        )
        val rotYaw = RotationUtils.toRotation(position, false).yaw
        return rotYaw.toDouble()
    }
    private fun foundPitch(): Double {
        val found = false
        var findPitch = 0.0
        for (searchPitch in -90..90 step 0.1.toInt()) {
            if (calculateEnderPearlLandingSpot(foundYaw().toFloat(), searchPitch.toFloat()) != null && !found) {
                findPitch = searchPitch.toDouble()
                break
            }
        }
        return findPitch
    }

    override val tag: String
        get() = modeValue.get()
}
