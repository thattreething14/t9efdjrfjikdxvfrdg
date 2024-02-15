package site.albaniacraft.towns.commands

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import site.albaniacraft.towns.Towns
import site.albaniacraft.towns.towns.ResidentManager
import site.albaniacraft.towns.utils.enums.TownPermissions

class ResidentCommand(plugin: Towns) : CommandExecutor, TabCompleter {
    private val residentManager = ResidentManager(plugin)
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be executed by a player.")
            return true
        }
        if (!sender.hasPermission(TownPermissions.TOWN_RESIDENT.permission)) {
            sender.sendMessage("${ChatColor.DARK_RED}You do not have permission to perform this action.")
            return true
        }

        val targetPlayerName = if (args.isNotEmpty()) args[0] else null
        if (targetPlayerName == null || Bukkit.getPlayerExact(targetPlayerName) == null) {
            sender.sendMessage("${ChatColor.DARK_RED}Player Doesn't exist")
            return true
        }

        val stats = residentManager.getResidentStats(sender, targetPlayerName)

        stats.forEach { sender.sendMessage(it) }

        return true
    }


    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
        }
        return emptyList()
    }
}
