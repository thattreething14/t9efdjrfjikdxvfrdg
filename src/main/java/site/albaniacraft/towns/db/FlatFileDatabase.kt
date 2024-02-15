package site.albaniacraft.towns.db

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.slf4j.LoggerFactory
import java.io.File
// implementation later maybe possibly
class FlatFileDatabase(private val pluginDataFolder: File) {
    private val gson = Gson()

    private fun loadJsonFile(file: File): Map<String, Any>? {
        return try {
            val json = file.readText()
            gson.fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveJsonFile(file: File, data: Map<String, Any>) {
        val json = gson.toJson(data)
        file.writeText(json)
    }

    fun initializeTables() {
        // Implement initialization logic for flat file database
        // Example: Creating necessary JSON files
        val nodesFile = File(pluginDataFolder, "nodes.json")
        if (!nodesFile.exists()) {
            saveJsonFile(nodesFile, emptyMap())
        }

        val claimedChunksFile = File(pluginDataFolder, "claimed_chunks.json")
        if (!claimedChunksFile.exists()) {
            saveJsonFile(claimedChunksFile, emptyMap())
        }

        val ranksFile = File(pluginDataFolder, "town_ranks.json")
        if (!ranksFile.exists()) {
            saveJsonFile(ranksFile, emptyMap())
        }

        val townsFile = File(pluginDataFolder, "towns.json")
        if (!townsFile.exists()) {
            saveJsonFile(townsFile, emptyMap())
        }

        val invitationsFile = File(pluginDataFolder, "pending_invitations.json")
        if (!invitationsFile.exists()) {
            saveJsonFile(invitationsFile, emptyMap())
        }

        LoggerFactory.getLogger(FlatFileDatabase::class.java).info("Flat File Database initialized successfully.")
    }
}
