package site.albaniacraft.towns.listeners

import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.entity.Player
import site.albaniacraft.towns.nodes.NodesManager
import org.bukkit.Chunk
import org.bukkit.plugin.java.JavaPlugin

class NodeEnterLeaveListener(plugin: JavaPlugin) : Listener {
    private val nodesManager = NodesManager(plugin)
    private var lastNode: String? = null

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player: Player = event.player
        val from: Chunk = event.from.chunk
        val to: Chunk = event.to.chunk

        val fromNode = nodesManager.getNodeAtChunk(from)
        val toNode = nodesManager.getNodeAtChunk(to)

        if (fromNode != toNode) {
            val enteringNodeName = toNode?.nodeName
            val leavingNodeName = fromNode?.nodeName

            if (leavingNodeName != null) {
                sendTitle(player, "${ChatColor.RED}Leaving Node", "${ChatColor.YELLOW}$leavingNodeName")
            }

            if (enteringNodeName != null) {
                sendTitle(player, "${ChatColor.GREEN}Entering Node", "${ChatColor.YELLOW}$enteringNodeName")
            }
            lastNode = enteringNodeName
        }
    }

    private fun sendTitle(player: Player, title: String, subtitle: String) {
        player.sendTitle(title, subtitle, 10, 40, 10)
    }
}
