package site.albaniacraft.towns.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.sql.Connection
import java.sql.SQLException
class SQLDataBaseManager(pluginDataFolder: File) {

    private val dataSource: HikariDataSource

    init {
        val dbFolder = File(pluginDataFolder, "db")
        val databasePath = File(dbFolder, "database.db")

        if (!dbFolder.exists()) {
            // creates db if it doesnt exist kinda sketchy but wtaver
            val directoriesCreated = dbFolder.mkdirs()
            if (directoriesCreated) {
                println("Database directory created successfully.")
            } else {
                println("Failed to create database directory.")
            }
        }

        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:${databasePath.path}"
            driverClassName = "org.sqlite.JDBC"
            maximumPoolSize = 10
        }

        dataSource = HikariDataSource(config)

        try {
            dataSource.connection.use{_->}
        } catch (e: SQLException) {
            throw RuntimeException("Error initializing SQLite database", e)
        }
    }

    fun getConnection(): Connection {
        try {
            return dataSource.connection
        } catch (e: SQLException) {
            throw RuntimeException("Error getting database connection", e)
        }
    }

    fun close() {
        dataSource.close()
    }
}
