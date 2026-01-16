package org.valkyrienskies.tournament.items

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.internal.joints.*
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.core.internal.joints.VSJointId
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.tournament.blocks.RopeHookBlock
import org.valkyrienskies.tournament.TournamentBlocks
import org.valkyrienskies.tournament.TournamentConfig
import org.valkyrienskies.tournament.blockentity.RopeHookBlockEntity

class RopeItem : Item(
        Properties().stacksTo(1)
) {

    private var clickedPosition: BlockPos? = null
    private var clickedShipId: ShipId? = null
    private var ropeConstraintId: VSJointId? = null
    private var clickedEntity: RopeHookBlockEntity? = null

    override fun useOn(context: UseOnContext): InteractionResult {

        val level = context.level
        val blockPos = context.clickedPos.immutable()

        val shipID: ShipId? = context.level.getShipObjectManagingPos(blockPos)?.id

        if (level is ServerLevel) {
            // if its a hook block
            if (level.getBlockState(blockPos).block == TournamentBlocks.ROPE_HOOK.get()) {
                //hook it up
                connectRope(context, level.getBlockState(blockPos).block as RopeHookBlock, blockPos, shipID, level)

                println("  ROPE --> " + TournamentBlocks.ROPE_HOOK.get() + " < == > " + level.getBlockState(blockPos).block)

                return InteractionResult.CONSUME
            } else {
                println(" !ROPE --> " + TournamentBlocks.ROPE_HOOK.get() + " < != > " + level.getBlockState(blockPos).block)
            }
        }
        return super.useOn(context)
    }

    private fun connectRope(context: UseOnContext, hookBlock: RopeHookBlock, blockPos: BlockPos, shipId: ShipId?, level: ServerLevel) {
        if(clickedPosition != null) {

            // CONNECT FULL ROPE
            var otherShipId = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!
            var thisShipId = otherShipId

            if (shipId != null)
                otherShipId = shipId
            if (clickedShipId != null)
                thisShipId = clickedShipId as ShipId

            println("other $otherShipId")
            println("this $thisShipId")

            if (clickedPosition == null)
                clickedPosition = blockPos

            val posA = clickedPosition!!.toJOMLD().add(0.5, 0.5, 0.5)
            val posB = blockPos.toJOMLD().add(0.5, 0.5, 0.5)

            var posC = clickedPosition!!.toJOMLD().add(0.5, 0.5, 0.5)
            var posD = blockPos.toJOMLD().add(0.5, 0.5, 0.5)

            if(level.getShipObjectManagingPos(clickedPosition!!) != null)
                posC = level
                    .getShipObjectManagingPos(clickedPosition!!)!!
                    .transform
                    .shipToWorld
                    .transformPosition(clickedPosition!!.toJOMLD())

            if(level.getShipObjectManagingPos(blockPos) != null)
                posD = level
                    .getShipObjectManagingPos(blockPos)!!
                    .transform
                    .shipToWorld
                    .transformPosition(blockPos.toJOMLD())

            println("A1 $posA")
            println("B1 $posB")
            println("C1 $posC")
            println("D1 $posD")

            val ropeCompliance = 1e-5 / (level.getShipObjectManagingPos(blockPos)?.inertiaData?.mass ?: 1).toDouble()
            val ropeMaxForce = TournamentConfig.SERVER.ropeMaxForce
            val ropeConstraint = VSDistanceJoint(
                thisShipId,
                VSJointPose(posA, Quaterniond()),
                otherShipId,
                VSJointPose(posB, Quaterniond()),
                VSJointMaxForceTorque(ropeMaxForce.toFloat(), ropeMaxForce.toFloat()),
                ropeCompliance,
                0.0f,
                (posC.sub(posD).length() + 1.0).toFloat()
            )

            println("Length: "+ posC.sub(posD).length())
            println(ropeConstraint)
            println(blockPos.toJOMLD())
            println(clickedEntity)
            val targetEntity = level.getBlockEntity(blockPos) as RopeHookBlockEntity

            ValkyrienSkiesMod.getOrCreateGTPA(dimensionId = level.dimensionId).addJoint(ropeConstraint) { id ->
                this.ropeConstraintId = id
                ropeConstraintId?.let {
                    targetEntity!!.setRopeID(it, posA, posB, level)
                    clickedEntity!!.setSecondary(blockPos)
                }

                clickedPosition = null
                clickedShipId = null
                this.ropeConstraintId = null

                println("Done\n")
                context.player!!.sendSystemMessage(Component.translatable("chat.vs_tournament.rope.connected"))
            }

        } else {

            // CONNECT FIRST POINT
            clickedShipId = shipId
            clickedPosition = blockPos
            ropeConstraintId = null
            clickedEntity = level.getBlockEntity(clickedPosition!!) as RopeHookBlockEntity
            context.player!!.sendSystemMessage(Component.translatable("chat.vs_tournament.rope.first"))

        }
    }

}