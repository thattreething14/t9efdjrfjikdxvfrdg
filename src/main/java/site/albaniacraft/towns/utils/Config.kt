package site.albaniacraft.towns.utils

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import site.albaniacraft.towns.Towns
import site.albaniacraft.towns.constants.TownRank
import java.io.File
class Config(private val plugin: JavaPlugin) {
    init {
        if (!configFileExists()) {
            plugin.saveDefaultConfig()
        }
    }

    private fun configFileExists(): Boolean {
        val configFile = File(plugin.dataFolder, "config.yml")
        return configFile.exists()
    }
    fun assignRankPermissions(playerName: String, newRank: TownRank) {
        val player = plugin.server.getPlayerExact(playerName) ?: return
        clearPlayerPermissions(player)
        if (plugin.config.isConfigurationSection("ranks")) {
            val ranksSection = plugin.config.getConfigurationSection("ranks")
            val defaultRankPermissions = ranksSection?.getStringList("MEMBER.permissions") ?: emptyList()

            val rankPermissions = ranksSection?.getStringList("$newRank.permissions") ?: defaultRankPermissions

            for (permission in rankPermissions) {
                player.addAttachment(plugin, permission, true)
            }
        }
    }
    fun assignRankPermissions(playerName: String, permissions: List<String>) {
        val player = plugin.server.getPlayerExact(playerName) ?: return
        clearPlayerPermissions(player)

        for (permission in permissions) {
            player.addAttachment(plugin, permission, true)
        }
    }

    private fun clearPlayerPermissions(player: Player) {
        val permissions = player.addAttachment(plugin)
        for (perm in permissions.permissions.keys) {
            permissions.unsetPermission(perm)
        }

        player.removeAttachment(permissions)
        player.addAttachment(plugin, 0)
    }
    fun getRankPermissions(rank: TownRank): List<String> {
        if (plugin.config.isConfigurationSection("ranks")) {
            val ranksSection = plugin.config.getConfigurationSection("ranks")
            val defaultRankPermissions = ranksSection?.getStringList("MEMBER.permissions") ?: emptyList()

            return ranksSection?.getStringList("${rank.name}.permissions") ?: defaultRankPermissions
        }

        return emptyList()
    }
    // TODO: implement cooldown for towns
    fun getNewTownCooldown(): Int {
        return plugin.config.getInt("towncreatecooldown", 0)
    }
    fun getTownCreateCost(): Double {
        return plugin.config.getDouble("towncreatecost")
    }
    fun getTownChunkCost(): Double {
        return plugin.config.getDouble("chunkcost")
    }
    fun getBaseChunks(): Int {
        return plugin.config.getInt("basechunks")
    }

    fun getChunksPerMember(): Double {
        return plugin.config.getDouble("chunkspermember")
    }
}

