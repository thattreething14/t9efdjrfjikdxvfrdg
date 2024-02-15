package site.albaniacraft.towns.listeners

import org.bukkit.ChatColor
import org.bukkit.Chunk
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin
import site.albaniacraft.towns.towns.TownManager

class TownClaimProtectionListener(plugin: JavaPlugin) : Listener {
    private val townManager = TownManager(plugin)
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        handleProtectionEvent(event.player, event.block.chunk, event)
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        handleProtectionEvent(event.player, event.block.chunk, event)
    }

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        event.blockList().forEach { block ->
            handleProtectionEvent(null, block.chunk, event)
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        handleProtectionEvent(event.player, event.clickedBlock?.chunk, event)
    }
    private fun handleProtectionEvent(player: Player?, chunk: Chunk?, event: Cancellable) {
        if (player == null || chunk == null) {
            return
        }

        val townName = townManager.getTownNameByLocation(chunk)
        val playerTownName = townManager.getTownByPlayer(player.name)

        if (townName == null) {
            return
        }

        if (townName == playerTownName) {
            return
        }

        player.sendMessage("${ChatColor.DARK_RED}[Towns] ${ChatColor.RED}You don't have permission to perform this action in this town.")
        event.isCancelled = true
    }
}
