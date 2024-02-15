package site.albaniacraft.towns.db

import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException
class DatabaseInitializer(plugin: JavaPlugin) {
    private val sqlDataManager = SQLDataBaseManager(plugin.dataFolder)
    private val logger = LoggerFactory.getLogger(DatabaseInitializer::class.java)

    fun initializeTables() {
        sqlDataManager.getConnection().use { connection ->
            try {
                // node tables
                initializeNodesTable(connection)
                initializeClaimedChunksTable(connection)
                // town tables
                initializeTownsTable(connection)
                initializeRanksTable(connection)
                initializePendingInvitationsTable(connection)
                initializeBalancesTable(connection)
                initializeChunkClaimedTable(connection)
                initializeTownSetsTable(connection)
            } catch (e: SQLException) {
                logger.error("Error initializing tables", e)
            }
        }
    }
    private fun initializeNodesTable(connection: Connection) {
        val createNodesTableQuery =
            "CREATE TABLE IF NOT EXISTS nodes(" +
                    "name TEXT PRIMARY KEY)"

        connection.prepareStatement(createNodesTableQuery).use { statement ->
            statement.executeUpdate()
        }
        logger.info("Nodes table created successfully.")
    }

    private fun initializeClaimedChunksTable(connection: Connection) {
        val createClaimedChunksTableQuery =
            "CREATE TABLE IF NOT EXISTS claim_chunks(" +
                    "node_name TEXT," +
                    "chunk_x INTEGER," +
                    "chunk_z INTEGER," +
                    "PRIMARY KEY (node_name, chunk_x, chunk_z))"

        connection.prepareStatement(createClaimedChunksTableQuery).use { statement ->
            statement.executeUpdate()
        }

        logger.info("Claimed Chunks table created successfully.")
    }
    private fun initializeBalancesTable(connection: Connection) {
        val createBalancesTableQuery =
            "CREATE TABLE IF NOT EXISTS balances(" +
                    "town_name TEXT PRIMARY KEY," +
                    "balance DOUBLE DEFAULT 0)"
        connection.prepareStatement(createBalancesTableQuery).use { statement ->
            statement.executeUpdate()
        }

        logger.info("Balances table created successfully.")
    }
    private fun initializeRanksTable(connection: Connection) {
        val createRanksTableQuery =
            "CREATE TABLE IF NOT EXISTS town_ranks(" +
                    "town_name TEXT," +
                    "player_name TEXT," +
                    "rank TEXT," +
                    "priority INTEGER," +
                    "PRIMARY KEY (town_name, player_name, rank))"
        connection.prepareStatement(createRanksTableQuery).use { statement ->
            statement.executeUpdate()
        }

        logger.info("Ranks table created successfully.")
    }

    private fun initializeTownsTable(connection: Connection) {
        val createTownsTableQuery =
            "CREATE TABLE IF NOT EXISTS towns(" +
                    "name TEXT PRIMARY KEY," +
                    "mayor TEXT," +
                    "members TEXT," +
                    "max_chunks INT)"
        connection.prepareStatement(createTownsTableQuery).use { statement ->
            statement.executeUpdate()
        }

        logger.info("Towns table created successfully.")
    }
    private fun initializePendingInvitationsTable(connection: Connection) {
        val createPendingInvitationsTableQuery =
            "CREATE TABLE IF NOT EXISTS pending_invitations(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "invited_player_name TEXT NOT NULL," +
                    "sender_name TEXT NOT NULL," +
                    "town_name TEXT NOT NULL," +
                    "timestamp INTEGER NOT NULL DEFAULT 0)"
        connection.prepareStatement(createPendingInvitationsTableQuery).use { statement ->
            statement.executeUpdate()
        }

        logger.info("Pending Invitations table created successfully.")
    }
    private fun initializeChunkClaimedTable(connection: Connection) {
        val createChunkClaimedTableQuery =
            "CREATE TABLE IF NOT EXISTS town_chunks(" +
                    "town_name TEXT," +
                    "world_name TEXT," +
                    "x INTEGER," +
                    "z INTEGER," +
                    "unclaimable BOOLEAN DEFAULT 0," +
                    "PRIMARY KEY (town_name, world_name, x, z))"
        connection.prepareStatement(createChunkClaimedTableQuery).use { statement ->
            statement.executeUpdate()
        }

        logger.info("Chunk Claimed table created successfully.")
    }
    private fun initializeTownSetsTable(connection: Connection) {
        val createTownSetsTableQuery =
            "CREATE TABLE IF NOT EXISTS town_sets(" +
                    "town_name TEXT PRIMARY KEY," +
                    "description TEXT)"

        connection.prepareStatement(createTownSetsTableQuery).use { statement ->
            statement.executeUpdate()
        }

        logger.info("Town Sets table created successfully.")
    }
}
