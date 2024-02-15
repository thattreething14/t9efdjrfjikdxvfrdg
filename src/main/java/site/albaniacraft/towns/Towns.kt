package site.albaniacraft.towns

import net.milkbowl.vault.chat.Chat
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.permission.Permission

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator

import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.java.JavaPlugin

import site.albaniacraft.towns.commands.TownCommand
import site.albaniacraft.towns.commands.ResidentCommand
import site.albaniacraft.towns.commands.NodesCommand
import site.albaniacraft.towns.db.DatabaseInitializer
import site.albaniacraft.towns.db.SQLDataBaseManager
import site.albaniacraft.towns.listeners.NodeEnterLeaveListener
import site.albaniacraft.towns.listeners.PaintBrushListener
import site.albaniacraft.towns.listeners.TownClaimProtectionListener
import site.albaniacraft.towns.listeners.TownEnterLeaveListener
import site.albaniacraft.towns.nodes.NodesManager
import site.albaniacraft.towns.towns.ResidentManager
import site.albaniacraft.towns.towns.TownManager
import site.albaniacraft.towns.towns.UpkeepManager

class Towns : JavaPlugin() {
    private lateinit var sqlDataManager: SQLDataBaseManager
    private lateinit var nodesManager: NodesManager
    private lateinit var townManager: TownManager
    private lateinit var residentManager: ResidentManager
    private lateinit var databaseInitializer: DatabaseInitializer
    private lateinit var upkeepManager: UpkeepManager
    private var econ: Economy? = null
    private var perms: Permission? = null
    private var chat: Chat? = null
    override fun onEnable() {
        configureHikariLogger()
        initializeSQLDataManager()
        initializeDatabaseInitializer()
        initializeManagers()
        if (!setupEconomy()) {
            logger.severe("- Disabled due to no Vault dependency found!")
            server.pluginManager.disablePlugin(this)
            return
        }
        logger.info("Successfully hooked into the Vault plugin!")
        setupPermissions()
        setupChat()
        registerCommands()
        registerListeners()
        upkeepManager.scheduleUpkeep()
    }

    override fun onDisable() {
        if (::sqlDataManager.isInitialized) {
            sqlDataManager.close()
        }
    }
    private fun configureHikariLogger() {
        val hikariLogger = LogManager.getLogger("com.zaxxer.hikari")
        Configurator.setLevel(hikariLogger.name, Level.OFF)
    }

    private fun initializeManagers() {
        nodesManager = NodesManager(this)
        townManager = TownManager(this)
        residentManager = ResidentManager(this)
        upkeepManager = UpkeepManager(this)
    }
    private fun initializeDatabaseInitializer() {
        databaseInitializer = DatabaseInitializer(this)
        databaseInitializer.initializeTables()
    }

    private fun initializeSQLDataManager() {
        if (!::sqlDataManager.isInitialized) {
            sqlDataManager = SQLDataBaseManager(dataFolder)
        }
    }
    private fun registerCommands() {
        val nodesCommand = NodesCommand(this)
        getCommand("nodes")?.setExecutor(nodesCommand)
        getCommand("nodes")?.tabCompleter = nodesCommand

        val townCommand = TownCommand(this)
        getCommand("town")?.setExecutor(townCommand)
        getCommand("town")?.tabCompleter = townCommand

        val residentCommand = ResidentCommand(this)
        getCommand("resident")?.setExecutor(residentCommand)
        getCommand("resident")?.tabCompleter = residentCommand
    }
    private fun registerListeners() {
        val paintBrushListener = PaintBrushListener(this)
        server.pluginManager.registerEvents(paintBrushListener, this)

        val nodeEnterLeaveListener = NodeEnterLeaveListener(this)
        server.pluginManager.registerEvents(nodeEnterLeaveListener, this)

        val townEnterLeaveListener = TownEnterLeaveListener(this)
        server.pluginManager.registerEvents(townEnterLeaveListener, this)

        val townClaimProtectionListener = TownClaimProtectionListener(this)
        server.pluginManager.registerEvents(townClaimProtectionListener, this)
    }
    private fun setupEconomy(): Boolean {
        if (server.pluginManager.getPlugin("Vault") == null) {
            return false
        }
        val rsp: RegisteredServiceProvider<Economy>? = server.servicesManager.getRegistration(Economy::class.java)
        econ = rsp?.provider
        return econ != null
    }

    private fun setupChat(): Boolean {
        val rsp: RegisteredServiceProvider<Chat>? = server.servicesManager.getRegistration(Chat::class.java)
        chat = rsp?.provider
        return chat != null
    }

    private fun setupPermissions(): Boolean {
        val rsp: RegisteredServiceProvider<Permission>? = server.servicesManager.getRegistration(Permission::class.java)
        perms = rsp?.provider
        return perms != null
    }
    fun getEconomy(): Economy? {
        return econ
    }

    fun getPermissions(): Permission? {
        return perms
    }

    fun getChat(): Chat? {
        return chat
    }
}
