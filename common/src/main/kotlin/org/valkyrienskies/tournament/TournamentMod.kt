package org.valkyrienskies.tournament

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.attachment.*
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.api.attachment.AttachmentHolder
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.tournament.ship.*
import org.valkyrienskies.tournament.util.extension.with

object TournamentMod {
    const val MOD_ID = "vs_tournament"

    @OptIn(GameTickOnly::class, VsBeta::class)
    @JvmStatic
    fun init() {
        // VSConfigClass.registerConfig("vs_tournament", TournamentConfig::class.java)
        TournamentBlocks.register()
        TournamentBlockEntities.register()
        TournamentItems.register()
        TournamentWeights.register()
        TournamentTriggers.init()

        vsApi.registerAttachment(BalloonShipControl::class.java) { useTransientSerializer()}
        vsApi.registerAttachment(PulseShipControl::class.java) { useTransientSerializer()}
        vsApi.registerAttachment(SpinnerShipControl::class.java) { useTransientSerializer()}
        vsApi.registerAttachment(ThrusterShipControl::class.java) { useTransientSerializer()}
        vsApi.registerAttachment(tournamentShipControl::class.java) { useTransientSerializer()}
        vsApi.registerAttachment(TournamentShips::class.java) { useTransientSerializer()}

        vsApi.shipLoadEvent.on { e ->
            val ship = e.ship

            if (TournamentConfig.SERVER.removeAllAttachments) {
                if (ship is LoadedServerShip) {
                    ship.removeAttachment<BalloonShipControl>()
                    ship.removeAttachment<PulseShipControl>()
                    ship.removeAttachment<SpinnerShipControl>()
                    ship.removeAttachment<ThrusterShipControl>()
                    ship.removeAttachment<tournamentShipControl>()
                    ship.removeAttachment<TournamentShips>()
                }
            }
            else {
                val thrusterShipCtrl = ship.getAttachment<ThrusterShipControl>()
                if (thrusterShipCtrl != null) {
                    TournamentShips.getOrCreate(ship).addThrusters(thrusterShipCtrl.Thrusters.with(thrusterShipCtrl.thrusters))
                    ship.removeAttachment<ThrusterShipControl>()
                }

                val balloonShipCtrl = ship.getAttachment<BalloonShipControl>()
                if (balloonShipCtrl != null) {
                    TournamentShips.getOrCreate(ship).addBalloons(balloonShipCtrl.balloons)
                    ship.removeAttachment<BalloonShipControl>()
                }

                val spinnerShipCtrl = ship.getAttachment<SpinnerShipControl>()
                if (spinnerShipCtrl != null) {
                    TournamentShips.getOrCreate(ship).addSpinners(spinnerShipCtrl.spinners.with(spinnerShipCtrl.Spinners))
                    ship.removeAttachment<SpinnerShipControl>()
                }

                val pulsesShipCtrl = ship.getAttachment<PulseShipControl>()
                if (pulsesShipCtrl != null) {
                    pulsesShipCtrl.addToNew(TournamentShips.getOrCreate(ship))
                    ship.removeAttachment<PulseShipControl>()
                }
            }
        }
    }

    @JvmStatic
    fun initClient() {

    }

    interface ClientRenderers {
        fun <T: BlockEntity> registerBlockEntityRenderer(t: BlockEntityType<T>, r: BlockEntityRendererProvider<T>)
    }

    @JvmStatic
    fun initClientRenderers(clientRenderers: ClientRenderers) {
        TournamentBlockEntities.initClientRenderers(clientRenderers)
    }
}
