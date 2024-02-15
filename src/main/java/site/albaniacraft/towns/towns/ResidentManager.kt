package site.albaniacraft.towns.towns

import org.bukkit.ChatColor
import org.bukkit.entity.Player
import site.albaniacraft.towns.Towns

class ResidentManager(private val plugin: Towns) {
    private val townManager = TownManager(plugin)
    fun getResidentStats(sender: Player, targetPlayerName: String?): List<String> {
        val playerName = targetPlayerName ?: sender.name
        val townName = townManager.getTownByPlayer(playerName)
        val playerRank = townName?.let { townManager.getPlayerRank(playerName, it) }
        val playerBalance = plugin.getEconomy()?.getBalance(playerName) ?: 0.0

        val stats = mutableListOf<String>()
        stats.add("${ChatColor.DARK_GREEN}=== Town Stats for ${ChatColor.GREEN}$playerName ${ChatColor.DARK_GREEN}===")
        stats.add("${ChatColor.DARK_GREEN}Town: ${ChatColor.GREEN}${townName ?: "None"}")
        stats.add("${ChatColor.DARK_GREEN}Rank: ${ChatColor.GREEN}${playerRank?.name ?: "None"}")
        stats.add("${ChatColor.DARK_GREEN}Balance: ${ChatColor.GREEN}$playerBalance")

        return stats
    }
}
