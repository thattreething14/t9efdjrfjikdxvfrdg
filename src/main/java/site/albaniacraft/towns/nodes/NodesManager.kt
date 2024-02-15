package site.albaniacraft.towns.nodes

import org.bukkit.Chunk
import org.bukkit.plugin.java.JavaPlugin
import site.albaniacraft.towns.db.SQLDataBaseManager
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

class NodesManager(plugin: JavaPlugin) {
    private val sqlDataManager = SQLDataBaseManager(plugin.dataFolder)
    /**
    * to lazy to put that much notes here however
    * this basically just takes table info from db and does shit with them
    **/

    data class Node(val nodeName: String)
    fun getNodeAtChunk(chunk: Chunk): Node? {
        try {
            sqlDataManager.getConnection().use { connection ->
                val query = "SELECT node_name FROM claim_chunks WHERE chunk_x = ? AND chunk_z = ?"

                connection.prepareStatement(query).use { statement ->
                    statement.setInt(1, chunk.x)
                    statement.setInt(2, chunk.z)

                    val resultSet: ResultSet = statement.executeQuery()
                    if (resultSet.next()) {
                        val nodeName = resultSet.getString("node_name")
                        return Node(nodeName)
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }

        return null
    }

    fun createNode(name: String) {
        sqlDataManager.getConnection().use { connection ->
            try {
                val query = "INSERT INTO nodes (name) VALUES (?)"
                connection.prepareStatement(query).use { statement ->
                    statement.setString(1, name)
                    statement.executeUpdate()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    fun deleteNode(name: String) {
        sqlDataManager.getConnection().use { connection ->
            try {
                deleteClaimedChunks(connection, name)
                val nodeDeleteQuery = "DELETE FROM nodes WHERE name = ?"
                connection.prepareStatement(nodeDeleteQuery).use { statement ->
                    statement.setString(1, name)
                    statement.executeUpdate()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    private fun deleteClaimedChunks(connection: Connection, nodeName: String) {
        try {
            val deleteChunksQuery = "DELETE FROM claim_chunks WHERE node_name = ?"
            connection.prepareStatement(deleteChunksQuery).use { statement ->
                statement.setString(1, nodeName)
                statement.executeUpdate()
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    fun nodeExists(name: String): Boolean {
        sqlDataManager.getConnection().use { connection ->
            try {
                val query = "SELECT COUNT(*) FROM nodes WHERE name = ?"
                connection.prepareStatement(query).use { statement ->
                    statement.setString(1, name)
                    val resultSet = statement.executeQuery()
                    return resultSet.getInt(1) > 0
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
        return false
    }
    fun getNodeNameFromClaimedChunk(chunk: Chunk): String? {
        try {
            sqlDataManager.getConnection().use { connection ->
                val query =
                    "SELECT node_name FROM claim_chunks WHERE chunk_x = ? AND chunk_z = ?"

                connection.prepareStatement(query).use { statement ->
                    statement.setInt(1, chunk.x)
                    statement.setInt(2, chunk.z)

                    val resultSet: ResultSet = statement.executeQuery()
                    if (resultSet.next()) {
                        return resultSet.getString("node_name")
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return null
    }
    fun getNodeList(): List<String> {
        val nodeList = mutableListOf<String>()
        sqlDataManager.getConnection().use { connection ->
            try {
                val query = "SELECT name FROM nodes"
                connection.prepareStatement(query).use { statement ->
                    val resultSet = statement.executeQuery()
                    while (resultSet.next()) {
                        nodeList.add(resultSet.getString("name"))
                    }
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
        return nodeList
    }

    fun claimChunk(nodeName: String, chunk: Chunk): Boolean {
        val x = chunk.x
        val z = chunk.z

        try {
            sqlDataManager.getConnection().use { connection ->
                if (!nodeExists(nodeName)) {
                    return false
                }

                if (isChunkClaimed(nodeName, x, z) || isChunkClaimedOtherNode(x, z, nodeName)) {
                    return false
                }

                val insertChunkQuery =
                    "INSERT INTO claim_chunks(node_name, chunk_x, chunk_z) VALUES (?, ?, ?)"

                connection.prepareStatement(insertChunkQuery).use { statement ->
                    statement.setString(1, nodeName)
                    statement.setInt(2, x)
                    statement.setInt(3, z)
                    statement.executeUpdate()
                }

                return true
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return false
        }
    }

    fun isChunkClaimedOtherNode(x: Int, z: Int, currentNodeName: String): Boolean {
        try {
            sqlDataManager.getConnection().use { connection ->
                val checkClaimedQuery =
                    "SELECT * FROM claim_chunks WHERE chunk_x = ? AND chunk_z = ? AND node_name <> ?"

                connection.prepareStatement(checkClaimedQuery).use { statement ->
                    statement.setInt(1, x)
                    statement.setInt(2, z)
                    statement.setString(3, currentNodeName)
                    val resultSet = statement.executeQuery()

                    return resultSet.next()
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return false
        }
    }
    fun isChunkClaimed(nodeName: String, x: Int, z: Int): Boolean {
        try {
            sqlDataManager.getConnection().use { connection ->
                val checkClaimedQuery =
                    "SELECT * FROM claim_chunks WHERE node_name = ? AND chunk_x = ? AND chunk_z = ?"

                connection.prepareStatement(checkClaimedQuery).use { statement ->
                    statement.setString(1, nodeName)
                    statement.setInt(2, x)
                    statement.setInt(3, z)
                    val resultSet = statement.executeQuery()

                    return resultSet.next()
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return false
        }
    }

    fun unclaimChunk(nodeName: String, chunk: Chunk): Boolean {
        val x = chunk.x
        val z = chunk.z

        try {
            sqlDataManager.getConnection().use { connection ->
                if (!nodeExists(nodeName)) {
                    return false
                }

                if (!isChunkClaimed(nodeName, x, z)) {
                    return false
                }

                val deleteChunkQuery =
                    "DELETE FROM claim_chunks WHERE node_name = ? AND chunk_x = ? AND chunk_z = ?"

                connection.prepareStatement(deleteChunkQuery).use { statement ->
                    statement.setString(1, nodeName)
                    statement.setInt(2, x)
                    statement.setInt(3, z)
                    statement.executeUpdate()
                }

                return true
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return false
        }
    }
}