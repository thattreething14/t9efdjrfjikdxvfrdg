package site.albaniacraft.towns.listeners

import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.java.JavaPlugin
import site.albaniacraft.towns.towns.TownManager

class TownEnterLeaveListener(plugin: JavaPlugin) : Listener {
    private val townManager = TownManager(plugin)
    private val playerTownMap = mutableMapOf<String, String?>()

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val fromChunk = event.from.chunk
        val toChunk = event.to.chunk

        // Check if the player has moved from one chunk to another
        if (fromChunk != toChunk) {
            val fromTown = townManager.getTownAtLocation(fromChunk.world.name, event.from)
            val toTown = townManager.getTownAtLocation(toChunk.world.name, event.to)

            val previousTown = playerTownMap[player.name]

            // Check if the player's town status has changed
            if (fromTown != previousTown || toTown != previousTown) {
                // Player left a town
                if (fromTown != null) {
                    player.sendTitle(
                        "${ChatColor.RED}Leaving Town",
                        "${ChatColor.GRAY}Goodbye, ${ChatColor.YELLOW}${fromTown}${ChatColor.GRAY}!",
                        10,
                        70,
                        20
                    )
                } else {
                    // Player left wilderness or another non-town area
                    player.sendTitle(
                        "${ChatColor.RED}Leaving Wilderness",
                        "${ChatColor.GRAY}You are now outside of any town.",
                        10,
                        70,
                        20
                    )
                }

                // Player entered a town
                if (toTown != null) {
                    player.sendTitle(
                        "${ChatColor.GREEN}Entering Town",
                        "${ChatColor.GRAY}Welcome to ${ChatColor.YELLOW}${toTown}${ChatColor.GRAY}!",
                        10,
                        70,
                        20
                    )
                } else {
                    // Player entered wilderness or another non-town area
                    player.sendTitle(
                        "${ChatColor.GREEN}Entering Wilderness",
                        "${ChatColor.GRAY}You are now in the wilderness.",
                        10,
                        70,
                        20
                    )
                }

                // Update the player's town status
                playerTownMap[player.name] = toTown
            }
        }
    }
}
