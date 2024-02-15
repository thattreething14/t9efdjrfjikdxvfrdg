package site.albaniacraft.towns.towns

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import site.albaniacraft.towns.constants.TownRank
import site.albaniacraft.towns.db.SQLDataBaseManager
import site.albaniacraft.towns.utils.Config
import java.sql.SQLException

/**
 * funny fortnite balls xd class file
 * BTW working on documentation probably going use chatgpt for
 * it because I don't want to write down everything.
 * @author me :D
 */
class TownManager(plugin: JavaPlugin) {
    private val pluginDataFolder = plugin.dataFolder
    private val sqlDataManager = SQLDataBaseManager(pluginDataFolder)
    private val config = Config(plugin)
    fun createTown(townName: String, mayor: String, members: List<String>) {
        val insertQuery = "INSERT INTO towns (name, mayor, members, max_chunks) VALUES (?, ?, ?, ?)"


        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(insertQuery).use { statement ->
                statement.setString(1, townName)
                statement.setString(2, mayor)
                statement.setString(3, members.joinToString(","))
                statement.executeUpdate()
            }
        }
    }
    fun getTownMayor(townName: String): String? {
        val selectQuery = "SELECT mayor FROM towns WHERE name = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(selectQuery).use { statement ->
                statement.setString(1, townName)

                statement.executeQuery().use { resultSet ->
                    return if (resultSet.next()) {
                        resultSet.getString("mayor")
                    } else {
                        null
                    }
                }
            }
        }
    }

    fun isTownExists(townName: String): Boolean {
        val query = "SELECT COUNT(*) FROM towns WHERE name = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, townName)

                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val count = resultSet.getInt(1)
                    return count > 0
                }
            }
        }

        return false
    }

    fun isPlayerInTown(playerName: String): Boolean {
        val query = "SELECT COUNT(*) FROM towns WHERE members LIKE ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, "%$playerName%")

                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val count = resultSet.getInt(1)
                    return count > 0
                }
            }
        }

        return false
    }

    fun getTownByPlayer(playerName: String): String? {
        val query = "SELECT name FROM towns WHERE members LIKE ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, "%$playerName%")

                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    return resultSet.getString("name")
                }
            }
        }
        return null
    }
    fun setTownMayor(townName: String, newMayor: String) {
        val updateQuery = "UPDATE towns SET mayor = ? WHERE name = ?"
        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(updateQuery).use { preparedStatement ->
                try {
                    preparedStatement.setString(1, newMayor)
                    preparedStatement.setString(2, townName)
                    preparedStatement.executeUpdate()
                } catch (e: SQLException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun isPlayerInvited(playerName: String, townName: String): Boolean {
        val query = "SELECT COUNT(*) FROM pending_invitations WHERE invited_player_name = ? AND town_name = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, playerName)
                statement.setString(2, townName)

                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val count = resultSet.getInt(1)
                    return count > 0
                }
            }
        }

        return false
    }
    fun getAllTowns(): List<String> {
        val towns = mutableListOf<String>()
        val query = "SELECT name FROM towns"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                val resultSet = statement.executeQuery()

                while (resultSet.next()) {
                    val townName = resultSet.getString("name")
                    towns.add(townName)
                }
            }
        }

        return towns
    }
    fun deleteTown(townName: String) {
        val deleteTownQuery = "DELETE FROM towns WHERE name = ?"
        val deleteRanksQuery = "DELETE FROM town_ranks WHERE town_name = ?"
        val deleteBalanceQuery = "DELETE FROM balances WHERE town_name = ?"
        val deleteClaimedChunkQuery = "DELETE FROM town_chunks WHERE town_name = ?"
        val deleteTownSetsQuery = "DELETE FROM town_sets WHERE town_name = ?"
        sqlDataManager.getConnection().use { connection ->
            // rank deletion
            connection.prepareStatement(deleteRanksQuery).use { deleteRanksStatement ->
                deleteRanksStatement.setString(1, townName)
                deleteRanksStatement.executeUpdate()
            }
            // balance deletion
            connection.prepareStatement(deleteBalanceQuery).use { deleteBalanceStatement ->
                deleteBalanceStatement.setString(1, townName)
                deleteBalanceStatement.executeUpdate()
            }
            // claimed chunk deletion
            connection.prepareStatement(deleteClaimedChunkQuery).use { deleteChunkStatement ->
                deleteChunkStatement.setString(1, townName)
                deleteChunkStatement.executeUpdate()
            }
            // town sets deletion
            connection.prepareStatement(deleteTownSetsQuery).use { deleteTownSetsStatement ->
                deleteTownSetsStatement.setString(1, townName)
                deleteTownSetsStatement.executeUpdate()
            }
            // town object deletion
            connection.prepareStatement(deleteTownQuery).use { deleteTownStatement ->
                deleteTownStatement.setString(1, townName)
                deleteTownStatement.executeUpdate()
            }
        }
    }

    fun getTownByMayor(mayorName: String): String? {
        val query = "SELECT name FROM towns WHERE mayor = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, mayorName)

                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    return resultSet.getString("name")
                }
            }
        }

        return null
    }

    private fun addPlayerToTown(playerName: String, townName: String) {
        val updateQuery = "UPDATE towns SET members = members || ? WHERE name = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(updateQuery).use { statement ->
                statement.setString(1, ",$playerName")
                statement.setString(2, townName)

                statement.executeUpdate()
            }
        }
    }
    fun setTownName(playerName: String, newName: String) {
        val oldTownName = getTownByPlayer(playerName) ?: return

        val query = "UPDATE towns SET name = ? WHERE mayor = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, newName)
                statement.setString(2, playerName)

                statement.executeUpdate()
            }
        }
        updateTownNameInRanks(oldTownName, newName)
        updateTownNameInBalances(oldTownName, newName)
        updateTownNameInPendingInvitations(oldTownName, newName)
        updateTownNameInTownChunks(oldTownName, newName)
        updateTownNameInSets(oldTownName, newName)
    }
    private fun updateTownNameInSets(oldName: String, newName: String) {
        val query = "UPDATE town_sets SET town_name = ? WHERE town_name = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, newName)
                statement.setString(2, oldName)

                statement.executeUpdate()
            }
        }
    }
    private fun updateTownNameInRanks(oldName: String, newName: String) {
        val query = "UPDATE town_ranks SET town_name = ? WHERE town_name = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, newName)
                statement.setString(2, oldName)

                statement.executeUpdate()
            }
        }
    }
    private fun updateTownNameInBalances(oldName: String, newName: String) {
        val query = "UPDATE balances SET town_name = ? WHERE town_name = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, newName)
                statement.setString(2, oldName)

                statement.executeUpdate()
            }
        }
    }
    private fun updateTownNameInPendingInvitations(oldName: String, newName: String) {
        val query = "UPDATE pending_invitations SET town_name = ? WHERE town_name = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, newName)
                statement.setString(2, oldName)

                statement.executeUpdate()
            }
        }
    }
    private fun updateTownNameInTownChunks(oldName: String, newName: String) {
        val query = "UPDATE town_chunks SET town_name = ? WHERE town_name = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, newName)
                statement.setString(2, oldName)

                statement.executeUpdate()
            }
        }
    }

    fun storeInvitation(senderName: String, invitedPlayerName: String, townName: String): Boolean {
        if (getInvitation(invitedPlayerName, townName) != null) {
            return false
        }

        return try {
            val currentTimestamp = System.currentTimeMillis()

            sqlDataManager.getConnection().use { connection ->
                val statement =
                    connection.prepareStatement("INSERT INTO pending_invitations (invited_player_name, sender_name, town_name, timestamp) VALUES (?, ?, ?, ?)")
                statement.setString(1, invitedPlayerName)
                statement.setString(2, senderName)
                statement.setString(3, townName)
                statement.setLong(4, currentTimestamp)
                statement.executeUpdate()
            }

            true
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }
    }

    fun acceptInvitation(playerName: String, townName: String): Boolean {
        val invitation = getInvitation(playerName, townName)

        if (invitation != null) {
            deleteInvitation(playerName, townName)
            addPlayerToTown(playerName, townName)
            return true
        }

        return false
    }

    fun declineInvitation(playerName: String, townName: String): Boolean {
        val invitation = getInvitation(playerName, townName)

        if (invitation != null) {
            deleteInvitation(playerName, townName)
            return true
        }

        return false
    }

    private fun getInvitation(playerName: String, townName: String): Long? {
        val query = "SELECT timestamp FROM pending_invitations WHERE invited_player_name = ? AND town_name = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, playerName)
                statement.setString(2, townName)

                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    return resultSet.getLong("timestamp")
                }
            }
        }

        return null
    }

    private fun deleteInvitation(playerName: String, townName: String) {
        val query = "DELETE FROM pending_invitations WHERE invited_player_name = ? AND town_name = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, playerName)
                statement.setString(2, townName)

                statement.executeUpdate()
            }
        }
    }

    fun hasPlayerRank(playerName: String, townName: String, rank: TownRank): Boolean {
        val query = "SELECT COUNT(*) FROM town_ranks WHERE town_name = ? AND player_name = ? AND rank = ?"

        return sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, townName)
                statement.setString(2, playerName)
                statement.setString(3, rank.name)

                val resultSet = statement.executeQuery()

                resultSet.next() && resultSet.getInt(1) > 0
            }
        }
    }

    fun getPlayerRank(playerName: String, townName: String): TownRank? {
        val query = "SELECT rank FROM town_ranks WHERE town_name = ? AND player_name = ? ORDER BY priority DESC LIMIT 1"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, townName)
                statement.setString(2, playerName)

                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val rankName = resultSet.getString("rank")
                    return try {
                        TownRank.valueOf(rankName)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
            }

        }

        return null
    }

    fun addPlayerToRank(playerName: String, townName: String, rank: TownRank) {
        val insertQuery = "INSERT INTO town_ranks (town_name, player_name, rank, priority) VALUES (?, ?, ?, ?)"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(insertQuery).use { statement ->
                statement.setString(1, townName)
                statement.setString(2, playerName)
                statement.setString(3, rank.name)
                statement.setInt(4, rank.priority)

                try {
                    statement.executeUpdate()
                } catch (e: SQLException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun removePlayerFromRank(playerName: String, townName: String, rank: TownRank) {
        val deleteQuery = "DELETE FROM town_ranks WHERE town_name = ? AND player_name = ? AND rank = ?"

        sqlDataManager.getConnection().use { connection ->
            try {
                connection.prepareStatement(deleteQuery).use { statement ->
                    statement.setString(1, townName)
                    statement.setString(2, playerName)
                    statement.setString(3, rank.name)
                    statement.executeUpdate()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    fun getTownMembers(townName: String): List<String> {
        val query = "SELECT members FROM towns WHERE name = ?"

        return sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, townName)

                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val membersString = resultSet.getString("members")
                    return membersString.split(",").filter { it.isNotBlank() }.map { it.trim() }
                }
            }
            emptyList()
        }
    }
    fun removePlayerFromTown(playerName: String, townName: String) {
        sqlDataManager.getConnection().use { connection ->
            try {
                // Update members by replacing the player name with an empty string
                val updateMembersQuery = "UPDATE towns SET members = REPLACE(members, ?, '') WHERE name = ?"
                connection.prepareStatement(updateMembersQuery).use { updateMembersStatement ->
                    updateMembersStatement.setString(1, playerName)
                    updateMembersStatement.setString(2, townName)
                    updateMembersStatement.executeUpdate()
                }
                config.assignRankPermissions(playerName, TownRank.MEMBER)
                val playerRanks = getAllPlayerRanks(playerName, townName)
                playerRanks.forEach { removePlayerFromRank(playerName, townName, it) }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }
    private fun getAllPlayerRanks(playerName: String, townName: String): List<TownRank> {
        val query = "SELECT rank FROM town_ranks WHERE town_name = ? AND player_name = ?"

        return sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, townName)
                statement.setString(2, playerName)

                val resultSet = statement.executeQuery()
                val ranks = mutableListOf<TownRank>()

                while (resultSet.next()) {
                    val rankName = resultSet.getString("rank")
                    val rank = TownRank.valueOf(rankName)
                    ranks.add(rank)
                }

                ranks
            }
        }
    }
    fun getTownBalance(townName: String): Double {
        val query = "SELECT balance FROM balances WHERE town_name = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, townName)

                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    return resultSet.getDouble("balance")
                }
            }
        }

        return 0.0
    }

    fun updateTownBalance(townName: String, newBalance: Double) {
        val query = "INSERT OR REPLACE INTO balances (town_name, balance) VALUES (?, ?)"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, townName)
                statement.setDouble(2, newBalance)

                statement.executeUpdate()
            }
        }
    }
    fun claimChunk(townName: String, chunk: Chunk): Boolean {
        try {
            val claimQuery = "INSERT INTO town_chunks (town_name, world_name, x, z) VALUES (?, ?, ?, ?)"

            sqlDataManager.getConnection().use { connection ->
                connection.prepareStatement(claimQuery).use { statement ->
                    statement.setString(1, townName)
                    statement.setString(2, chunk.world.name)
                    statement.setInt(3, chunk.x)
                    statement.setInt(4, chunk.z)

                    statement.executeUpdate()
                }
            }

            return true
        } catch (e: SQLException) {
            e.printStackTrace()
            return false
        }
    }

    fun unclaimChunk(townName: String, chunk: Chunk): Boolean {
        try {
            val unclaimQuery = "DELETE FROM town_chunks WHERE town_name = ? AND world_name = ? AND x = ? AND z = ?"

            sqlDataManager.getConnection().use { connection ->
                connection.prepareStatement(unclaimQuery).use { statement ->
                    statement.setString(1, townName)
                    statement.setString(2, chunk.world.name)
                    statement.setInt(3, chunk.x)
                    statement.setInt(4, chunk.z)

                    statement.executeUpdate()
                }
            }

            return true
        } catch (e: SQLException) {
            e.printStackTrace()
            return false
        }
    }
    fun isChunkClaimedByTown(townName: String, chunk: Chunk): Boolean {
        val query = "SELECT COUNT(*) FROM town_chunks WHERE town_name = ? AND world_name = ? AND x = ? AND z = ?"

        return try {
            sqlDataManager.getConnection().use { connection ->
                connection.prepareStatement(query).use { statement ->
                    statement.setString(1, townName)
                    statement.setString(2, chunk.world.name)
                    statement.setInt(3, chunk.x)
                    statement.setInt(4, chunk.z)

                    val resultSet = statement.executeQuery()

                    resultSet.next() && resultSet.getInt(1) > 0
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }
    }
    fun isChunkClaimed(chunk: Chunk): Boolean {
        val query = "SELECT COUNT(*) FROM town_chunks WHERE world_name = ? AND x = ? AND z = ?"

        return try {
            sqlDataManager.getConnection().use { connection ->
                connection.prepareStatement(query).use { statement ->
                    statement.setString(1, chunk.world.name)
                    statement.setInt(2, chunk.x)
                    statement.setInt(3, chunk.z)

                    val resultSet = statement.executeQuery()

                    resultSet.next() && resultSet.getInt(1) > 0
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }
    }

    fun markChunkAsUnclaimable(townName: String, chunk: Chunk) {
        val updateQuery = "UPDATE town_chunks SET unclaimable = 1 WHERE town_name = ? AND world_name = ? AND x = ? AND z = ? AND unclaimable = 0"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(updateQuery).use { statement ->
                statement.setString(1, townName)
                statement.setString(2, chunk.world.name)
                statement.setInt(3, chunk.x)
                statement.setInt(4, chunk.z)

                statement.executeUpdate()
            }
        }
    }
    fun isChunkUnclaimable(townName: String, chunk: Chunk): Boolean {
        val query = "SELECT unclaimable FROM town_chunks WHERE town_name = ? AND world_name = ? AND x = ? AND z = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, townName)
                statement.setString(2, chunk.world.name)
                statement.setInt(3, chunk.x)
                statement.setInt(4, chunk.z)

                val resultSet = statement.executeQuery()

                return if (resultSet.next()) {
                    resultSet.getInt("unclaimable") == 1
                } else {
                    true
                }
            }
        }
    }
    fun getAdjacentClaimedChunks(townName: String, chunk: Chunk): List<Chunk> {
        val claimedChunks = mutableListOf<Chunk>()
        val world = chunk.world
        val x = chunk.x
        val z = chunk.z
        val directions = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0))
        for ((dx, dz) in directions) {
            val adjacentChunk = world.getChunkAt(x + dx, z + dz)
            if (isChunkClaimedByTown(townName, adjacentChunk)) {
                claimedChunks.add(adjacentChunk)
            }
        }
        return claimedChunks
    }
    fun getTownClaimedChunks(townName: String): List<Chunk> {
        val chunks = mutableListOf<Chunk>()

        val selectQuery = "SELECT world_name, x, z FROM town_chunks WHERE town_name = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(selectQuery).use { statement ->
                statement.setString(1, townName)

                val resultSet = statement.executeQuery()

                while (resultSet.next()) {
                    val worldName = resultSet.getString("world_name")
                    val x = resultSet.getInt("x")
                    val z = resultSet.getInt("z")

                    val world = Bukkit.getWorld(worldName)
                    if (world != null) {
                        val chunk = world.getChunkAt(x, z)
                        chunks.add(chunk)
                    }
                }
            }
        }

        return chunks
    }
    fun getTownNameByLocation(chunk: Chunk): String? {
        val query = "SELECT town_name FROM town_chunks WHERE world_name = ? AND x = ? AND z = ?"

        return try {
            sqlDataManager.getConnection().use { connection ->
                connection.prepareStatement(query).use { statement ->
                    statement.setString(1, chunk.world.name)
                    statement.setInt(2, chunk.x)
                    statement.setInt(3, chunk.z)

                    val resultSet = statement.executeQuery()

                    if (resultSet.next()) {
                        resultSet.getString("town_name")
                    } else {
                        null
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            null
        }
    }


    fun setMaxChunksForTown(townName: String, maxChunks: Int) {
        val updateQuery = "UPDATE towns SET max_chunks = ? WHERE name = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(updateQuery).use { statement ->
                statement.setInt(1, maxChunks)
                statement.setString(2, townName)

                statement.executeUpdate()
            }
        }
    }
    fun getMaxChunksForTown(townName: String): Int {
        val query = "SELECT max_chunks FROM towns WHERE name = ?"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, townName)

                statement.executeQuery().use { resultSet ->
                    return if (resultSet.next()) {
                        resultSet.getInt("max_chunks")
                    } else {
                        0
                    }
                }
            }
        }
    }

    fun getPlayersWithRanks(townName: String, vararg ranks: TownRank): List<Pair<TownRank, List<String>>> {
        val query = "SELECT rank, player_name FROM town_ranks WHERE town_name = ? AND rank IN (${ranks.joinToString { "?" }})"

        return sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, townName)
                ranks.forEachIndexed { index, rank ->
                    statement.setString(index + 2, rank.name)
                }

                val resultSet = statement.executeQuery()

                val playersByRank = mutableMapOf<TownRank, MutableList<String>>()

                while (resultSet.next()) {
                    val rankName = resultSet.getString("rank")
                    val playerName = resultSet.getString("player_name")

                    val rank = try {
                        TownRank.valueOf(rankName)
                    } catch (e: IllegalArgumentException) {
                        continue
                    }

                    playersByRank.computeIfAbsent(rank) { mutableListOf() }.add(playerName)
                }

                playersByRank.toList()
            }
        }
    }

    fun getTownRanks(townName: String): List<TownRank> {
        val query = "SELECT DISTINCT rank FROM town_ranks WHERE town_name = ? ORDER BY priority DESC"

        return sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, townName)

                val resultSet = statement.executeQuery()
                val ranks = mutableListOf<TownRank>()

                while (resultSet.next()) {
                    val rankName = resultSet.getString("rank")
                    try {
                        val rank = TownRank.valueOf(rankName)
                        ranks.add(rank)
                    } catch (_: IllegalArgumentException) {
                    }
                }

                ranks
            }
        }
    }

    fun getTownInfo(townName: String): TownInfo? {
        val mayor = getTownMayor(townName) ?: return null
        val members = getTownMembers(townName)
        val balance = getTownBalance(townName)
        val claimedChunks = getTownClaimedChunks(townName)
        val ranks = getTownRanks(townName)

        return TownInfo(mayor, members, balance, claimedChunks, ranks)
    }

    data class TownInfo(
        val mayor: String,
        val members: List<String>,
        val balance: Double,
        val claimedChunks: List<Chunk>,
        val ranks: List<TownRank>
    )
    fun getTownAtLocation(worldName: String, location: Location): String? {
        val chunk = location.chunk
        val x = chunk.x
        val z = chunk.z

        val query = "SELECT town_name FROM town_chunks WHERE world_name = ? AND x = ? AND z = ?"

        return sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, worldName)
                statement.setInt(2, x)
                statement.setInt(3, z)

                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    return resultSet.getString("town_name")
                }
            }

            null
        }
    }
    fun setTownDescription(townName: String, description: String) {
        val query = "INSERT OR REPLACE INTO town_sets(town_name, description) VALUES (?, ?)"

        sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, townName)
                statement.setString(2, description)

                statement.executeUpdate()
            }
        }
    }
    fun getTownDescription(townName: String): String? {
        val query = "SELECT description FROM town_sets WHERE town_name = ?"

        return sqlDataManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setString(1, townName)

                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    resultSet.getString("description")
                } else {
                    null
                }
            }
        }
    }
}