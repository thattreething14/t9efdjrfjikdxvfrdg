package site.albaniacraft.towns.commands

import net.kyori.adventure.text.Component
import org.bukkit.ChatColor
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import site.albaniacraft.towns.Towns
import site.albaniacraft.towns.nodes.NodesManager
import java.util.*

class NodesCommand(private val plugin: JavaPlugin) : CommandExecutor, TabCompleter {
    private val nodesManager = NodesManager(plugin)
    /**
     * parses the shit into tab completer uwu
     * **/
    private val SUB_COMMANDS = arrayOf(
        "create",
        "delete",
        "list",
        "help",
        "claim",
        "unclaim",
        "claimbrush"
    )
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!(sender is Player) || !sender.isOp) {
            sender.sendMessage("${ChatColor.RED}You do not have permission to use this command.")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}Unknown subcommand. Do /nodes help for a list of commands :D")
            return true
        }
        val subCommand = args[0].lowercase(Locale.getDefault())

        when (subCommand) {
            "create" -> executeCreateCommand(sender, args)
            "delete" -> executeDeleteCommand(sender, args)
            "list" -> executeListCommand(sender)
            "help" -> executeHelpCommand(sender)
            "claim" -> executeClaimCommand(sender, args)
            "unclaim" -> executeUnclaimCommand(sender, args)
            "claimbrush" -> executeClaimBrushCommand(sender, args)
            else -> {
                sender.sendMessage("${ChatColor.RED}Unknown subcommand. Do /nodes help for a list of commands :D")
                return true
            }
        }

        return true
    }
    //btw chatcolor may be deprecated but im lazy
    private fun executeCreateCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /nodes create <name>")
            return
        }
        val nodeName = args[1]
        if (nodesManager.nodeExists(nodeName)) {
            sender.sendMessage("${ChatColor.RED}Node '$nodeName' already exists.")
        } else {
            nodesManager.createNode(nodeName)
            sender.sendMessage("Node '$nodeName' created successfully.")
        }
    }
    private fun executeClaimBrushCommand(sender: Player, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /nodes claimbrush <node>")
            return
        }

        val nodeName = args[1]

        if (nodesManager.nodeExists(nodeName)) {
            val brush = createClaimBrush(nodeName)
            sender.inventory.addItem(brush)
            sender.sendMessage("${ChatColor.GREEN}Claim Brush added to your inventory for node '$nodeName'.")
        } else {
            sender.sendMessage("${ChatColor.RED}Node '$nodeName' does not exist.")
        }
    }

    private fun createClaimBrush(nodeName: String): ItemStack {
        val brush = ItemStack(Material.GOLDEN_SHOVEL)
        val meta = brush.itemMeta
        meta.displayName(Component.text("${ChatColor.GREEN}Claim Brush"))
        meta.lore(listOf(Component.text("Node: $nodeName")))
        meta.persistentDataContainer.set(NamespacedKey(plugin, "nodeName"), PersistentDataType.STRING, nodeName)
        brush.itemMeta = meta
        return brush
    }

    private fun executeDeleteCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /nodes delete <name>")
            return
        }
        val nodeName = args[1]

        if (!nodesManager.nodeExists(nodeName)) {
            sender.sendMessage("${ChatColor.RED}Node '$nodeName' does not exist.")
            return
        }

        nodesManager.deleteNode(nodeName)
        sender.sendMessage("Node '$nodeName' deleted successfully.")
    }
    private fun executeListCommand(sender: CommandSender) {
        val nodeList = nodesManager.getNodeList()
        if (nodeList.isEmpty()) {
            sender.sendMessage("No nodes found.")
        } else {
            sender.sendMessage("${ChatColor.BLUE}Nodes: ${nodeList.joinToString(", ")}")
        }
    }
    private fun executeHelpCommand(sender: CommandSender) {
        sender.sendMessage("${ChatColor.BLUE}=== Nodes Plugin Help ===")
        sender.sendMessage("${ChatColor.BLUE}/nodes create <name> - Create a new node.")
        sender.sendMessage("${ChatColor.BLUE}/nodes delete <name> - Delete an existing node.")
        sender.sendMessage("${ChatColor.BLUE}/nodes list - List all nodes.")
        sender.sendMessage("${ChatColor.BLUE}/nodes help - Display this help message.")
    }
    private fun executeClaimCommand(sender: Player, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /nodes claim <node>")
            return
        }

        val nodeName = args[1]
        val playerChunk: Chunk = sender.location.chunk

        if (nodesManager.nodeExists(nodeName)) {
            if (nodesManager.claimChunk(nodeName, playerChunk)) {
                val x = playerChunk.x
                val z = playerChunk.z
                sender.sendMessage("${ChatColor.GREEN}Chunk ${ChatColor.DARK_GREEN}($x, $z) ${ChatColor.GREEN}claimed successfully for node '$nodeName'.")
            } else {
                sender.sendMessage("${ChatColor.RED}Failed to claim chunk. Please make sure the node exists.")
            }
        } else {
            sender.sendMessage("${ChatColor.RED}Node '$nodeName' does not exist.")
        }
    }

    private fun executeUnclaimCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /nodes unclaim <name>")
            return
        }

        val nodeName = args[1]

        if (!nodesManager.nodeExists(nodeName)) {
            sender.sendMessage("${ChatColor.RED}Node '$nodeName' does not exist.")
            return
        }

        val player = sender as? Player
        val chunk = player?.location?.chunk

        if (chunk != null) {
            if (nodesManager.unclaimChunk(nodeName, chunk)) {
                sender.sendMessage("${ChatColor.RED}Chunk ${ChatColor.DARK_RED}(${chunk.x}, ${chunk.z}) ${ChatColor.RED}unclaimed successfully for node '$nodeName'.")
            } else {
                sender.sendMessage("${ChatColor.RED}Chunk is not claimed for node '$nodeName'.")
            }
        } else {
            sender.sendMessage("${ChatColor.RED}You must be in a chunk to use this command.")
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return SUB_COMMANDS.filter { it.startsWith(args[0], ignoreCase = true) }
        } else if (args.size == 2 && (args[0].equals("delete", ignoreCase = true) || args[0].equals("claimbrush", ignoreCase = true) || args[0].equals("claim", ignoreCase = true) || args[0].equals("unclaim", ignoreCase = true))) {
            val nodeNames = nodesManager.getNodeList()
            return nodeNames.filter { it.startsWith(args[1], ignoreCase = true) }
        }
        return emptyList()
    }
}