package site.albaniacraft.towns.listeners

import org.bukkit.ChatColor
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import site.albaniacraft.towns.nodes.NodesManager

/**
 * goodluck understanding this one chief **/
class PaintBrushListener(private val plugin: JavaPlugin) : Listener {
    private val nodesManager = NodesManager(plugin)
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK || event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) {
            val player = event.player
            val itemInHand = player.inventory.itemInMainHand

            if (isClaimBrush(itemInHand)) {
                val nodeName = getNodeNameFromClaimBrush(itemInHand)

                if (nodeName != null) {
                    val targetBlock = player.getTargetBlock(null, 500)
                    val chunk = targetBlock.chunk

                    if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
                        if (nodesManager.nodeExists(nodeName)) {
                            if (!nodesManager.isChunkClaimed(nodeName, chunk.x, chunk.z)) {
                                val claimedByOtherNode = nodesManager.isChunkClaimedOtherNode(chunk.x, chunk.z, nodeName)

                                if (!claimedByOtherNode) {
                                    nodesManager.claimChunk(nodeName, chunk)
                                    player.sendMessage("${ChatColor.GREEN}Chunk ${ChatColor.DARK_GREEN}(${chunk.x}, ${chunk.z}) ${ChatColor.GREEN}claimed successfully for node '$nodeName'.")
                                } else {
                                    player.sendMessage("${ChatColor.RED}Cannot claim. Chunk ${ChatColor.DARK_RED}(${chunk.x}, ${chunk.z}) ${ChatColor.RED}is already claimed by a different node.")
                                    event.isCancelled = true
                                }
                            }
                        }
                    } else if (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) {
                        val existingNodeName = nodesManager.getNodeNameFromClaimedChunk(chunk)

                        if (existingNodeName != null && existingNodeName == nodeName) {
                            nodesManager.unclaimChunk(nodeName, chunk)
                            player.sendMessage("${ChatColor.RED}Chunk ${ChatColor.DARK_RED}(${chunk.x}, ${chunk.z}) ${ChatColor.RED}unclaimed successfully for node '$nodeName'.")
                        } else {
                            player.sendMessage("${ChatColor.YELLOW}Cannot unclaim. Chunk ${ChatColor.GOLD}(${chunk.x}, ${chunk.z}) ${ChatColor.YELLOW}does not belong to node '$nodeName'.")
                            event.isCancelled = true
                        }
                    }
                }
            }
        }
    }

    private fun isClaimBrush(item: ItemStack): Boolean {
        val meta = item.itemMeta
        return meta?.persistentDataContainer?.has(NamespacedKey(plugin, "nodeName"), PersistentDataType.STRING) == true
    }

    private fun getNodeNameFromClaimBrush(item: ItemStack): String? {
        val meta = item.itemMeta
        return meta?.persistentDataContainer?.get(NamespacedKey(plugin, "nodeName"), PersistentDataType.STRING)
    }
}