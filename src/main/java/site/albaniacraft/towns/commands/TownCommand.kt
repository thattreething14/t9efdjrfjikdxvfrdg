package site.albaniacraft.towns.commands
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import site.albaniacraft.towns.Towns
import site.albaniacraft.towns.constants.TownRank
import site.albaniacraft.towns.towns.TownManager
import site.albaniacraft.towns.utils.Config
import site.albaniacraft.towns.utils.enums.*
import site.albaniacraft.treesgui.GuiBuilder
import site.albaniacraft.treesgui.GuiListener
import site.albaniacraft.treesgui.ItemStackBuilder
import java.util.*
class TownCommand(private val plugin: Towns) : CommandExecutor, TabCompleter {
    private val townManager = TownManager(plugin)
    private val config = Config(plugin)
    private val SUB_COMMANDS = arrayOf(
        "new",
        "delete",
        "rank",
        "invite",
        "accept",
        "deny",
        "claim",
        "unclaim",
        "leave",
        "help",
        "deposit",
        "withdraw",
        "info",
        "kick",
        "set",
        "list",
        "online",
        "ranklist",
        "reslist",
        "gui"
    )
    private val SET_SUB_COMMANDS = arrayOf(
        "mayor",
        "name",
        "description"
    )
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!(sender is Player)) {
            sender.sendMessage(TownMessages.NO_PERMISSION.message)
            return true
        }

        if (args.isEmpty()) {
            executeHelpCommand(sender, 1)
            return true
        }

        val subCommand = args[0].lowercase(Locale.getDefault())

        when (subCommand) {
            "new" -> executeNewTownCommand(sender, args)
            "delete" -> executeDeleteTownCommand(sender)
            "rank" -> executeRankCommand(sender, args)
            "invite" -> executeInviteCommand(sender, args)
            "accept" -> executeAcceptCommand(sender, args)
            "deny" -> executeDenyCommand(sender, args)
            "leave" -> executeLeaveCommand(sender)
            "help" -> { val page = if (args.size > 1) { args[1].toIntOrNull() ?: 1 } else {1}
                executeHelpCommand(sender, page) }
            "deposit" -> executeDepositCommand(sender, args)
            "withdraw" -> executeWithdrawCommand(sender, args)
            "claim" -> executeClaimCommand(sender)
            "unclaim" -> executeUnclaimCommand(sender)
            "info" -> executeInfoCommand(sender, args)
            "kick" -> executeKickCommand(sender, args)
            "set" -> executeSetCommand(sender, args)
            "list" -> executeListCommand(sender, args)
            "online" -> executeOnlineCommand(sender, args)
            "ranklist" -> executeRankListCommand(sender, args)
            "reslist" -> executeResListCommand(sender, args)
            "gui" -> executeGuiCommand(sender, args)
            else -> {
                executeHelpCommand(sender, 1)
                return true
            }
        }

        return true
    }
    private fun executeGuiCommand(sender: Player, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /t gui <townName>")
            return
        }

        val townName = args[1]
        val townInfo = townManager.getTownInfo(townName)

        if (townInfo != null) {
            val gui = createTownGui(townName, townInfo, sender)
            gui.open(sender)
        } else {
            sender.sendMessage("${ChatColor.RED}Town $townName not found.")
        }
    }
    private fun createTownGui(townName: String, townInfo: TownManager.TownInfo, player: Player): GuiBuilder {
        val gui = GuiBuilder("${ChatColor.DARK_BLUE}Town Information - $townName", 27)
        gui.allowItemClick = false
        val onlineMembers = getOnlineMembersInTown(townName)
        val maxChunks = townManager.getMaxChunksForTown(townName)
        val townRanking = getTownRanking(townName)
        gui.setItem(10, ItemStackBuilder(Material.DIAMOND_SWORD).displayName("${ChatColor.BLUE}Mayor: ${ChatColor.AQUA}${townInfo.mayor}").build())
        gui.setItem(12, ItemStackBuilder(Material.GOLDEN_APPLE).displayName("${ChatColor.BLUE}Members [${onlineMembers.size}]: ${ChatColor.AQUA}${townInfo.members}").build())
        gui.setItem(13, ItemStackBuilder(Material.NETHERITE_BLOCK).displayName("${ChatColor.BLUE}Overall Power: ${ChatColor.AQUA}${townRanking.rank}").build())
        gui.setItem(14, ItemStackBuilder(Material.EMERALD).displayName("${ChatColor.BLUE}Balance: ${ChatColor.AQUA}${townInfo.balance}").build())
        gui.setItem(16, ItemStackBuilder(Material.GRASS_BLOCK).displayName("${ChatColor.BLUE}Claimed Chunks: ${ChatColor.AQUA}${townInfo.claimedChunks.size}/$maxChunks").build())

        Bukkit.getPluginManager().registerEvents(GuiListener(gui), plugin)
        gui.open(player)
        return gui
    }
    private fun getTownRanking(townName: String): TownRanking {
        val townMembersCount = townManager.getTownMembers(townName).size
        val allTowns = townManager.getAllTowns()

        // sorts towns by the number of members in descending order
        val sortedTowns = allTowns.sortedByDescending { townManager.getTownMembers(it).size }

        // finds the rank of the current town
        val townRank = sortedTowns.indexOf(townName) + 1

        return TownRanking(townRank, townMembersCount)
    }
    data class TownRanking(val rank: Int, val membersCount: Int)
    private fun executeListCommand(sender: Player, args: Array<out String>) {
        if (args.size < 3 || args[1].lowercase(Locale.getDefault()) != "by") {
            sender.sendMessage("${ChatColor.RED}Usage: /t list by <chunks|members|wealth>")
            return
        }

        val option = args[2].lowercase(Locale.getDefault())

        when (option) {
            "chunks" -> listTownsByChunks(sender)
            "members" -> listTownsByMembers(sender)
            "wealth" -> listTownsByWealth(sender)
            else -> {
                sender.sendMessage("${ChatColor.RED}Unknown option: $option")
                sender.sendMessage("${ChatColor.RED}Usage: /t list by <chunks|members|wealth>")
            }
        }
    }
    private fun listTownsByChunks(sender: Player) {
        val towns = townManager.getAllTowns()

        val sortedTowns = towns.sortedByDescending { town ->
            townManager.getTownClaimedChunks(town).size
        }

        displayTownList(sender, "Chunks", sortedTowns) { town ->
            "${townManager.getTownClaimedChunks(town).size} Chunks"
        }
    }
    private fun executeRankListCommand(sender: Player, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /t ranklist <townName>")
            return
        }

        val townName = args[1]

        if (townManager.isTownExists(townName)) {
            val townRanks = townManager.getTownRanks(townName)
            sender.sendMessage("${ChatColor.DARK_BLUE}Ranks in Town ${ChatColor.AQUA}$townName:")

            townRanks.forEach { rank ->
                val playersWithRank = townManager.getPlayersWithRanks(townName, rank)
                if (playersWithRank.isNotEmpty()) {
                    val playerList = playersWithRank.flatMap { it.second }
                    sender.sendMessage("${ChatColor.BLUE}${rank.name.toLowerCase()}s: ${ChatColor.AQUA}${playerList.joinToString(", ")}")
                }
            }
        } else {
            sender.sendMessage("${ChatColor.RED}The town $townName does not exist.")
        }
    }
    private fun executeResListCommand(sender: Player, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /t reslist <townName>")
            return
        }

        val townName = args[1]

        if (townManager.isTownExists(townName)) {
            val residents = townManager.getTownMembers(townName)
            val townSize = residents.size
            sender.sendMessage("${ChatColor.DARK_BLUE}Residents in Town ${ChatColor.AQUA}$townName [$townSize]:")

            if (residents.isNotEmpty()) {
                sender.sendMessage("${ChatColor.AQUA}${residents.joinToString(", ")}")
            } else {
                sender.sendMessage("${ChatColor.BLUE}No residents in the town.")
            }
        } else {
            sender.sendMessage("${ChatColor.RED}The town $townName does not exist.")
        }
    }
    private fun listTownsByMembers(sender: Player) {
        val towns = townManager.getAllTowns()

        val sortedTowns = towns.sortedByDescending { town ->
            townManager.getTownMembers(town).size
        }

        displayTownList(sender, "Members", sortedTowns) { town ->
            "${townManager.getTownMembers(town).size} Members"
        }
    }
    private fun listTownsByWealth(sender: Player) {
        val towns = townManager.getAllTowns()

        val sortedTowns = towns.sortedByDescending { town ->
            townManager.getTownBalance(town)
        }

        displayTownList(sender, "Wealth", sortedTowns) { town ->
            "${townManager.getTownBalance(town)} Wealth"
        }
    }
    private fun displayTownList(sender: Player, category: String, towns: List<String>, countProvider: (String) -> String) {
        sender.sendMessage("${ChatColor.DARK_BLUE}Top Towns by $category:")

        towns.take(10).forEachIndexed { index, town ->
            val townText = TextComponent("${ChatColor.AQUA}${index + 1}. $town - ${countProvider(town)}")
            townText.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/t info $town")
            sender.sendMessage(townText)
        }
    }
    private fun executeKickCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /t kick <playerName>")
            return
        }

        val targetPlayerName = args[1]
        val player = sender as? Player ?: return

        val townName = townManager.getTownByPlayer(player.name)

        if (townName != null) {
            if (sender.hasPermission(TownPermissions.TOWN_KICK_PLAYER.permission)) {
                val targetPlayer = Bukkit.getPlayerExact(targetPlayerName)

                if (targetPlayer != null) {
                    val targetPlayerTown = townManager.getTownByPlayer(targetPlayerName)

                    if (targetPlayerTown == townName) {
                        val senderRank = townManager.getPlayerRank(player.name, townName)
                        val targetRank = townManager.getPlayerRank(targetPlayerName, townName)
                        if (senderRank != null && targetRank != null && senderRank.priority > targetRank.priority) {
                            townManager.removePlayerFromTown(targetPlayerName, townName)
                            val townMembers = townManager.getTownMembers(townName)
                            townMembers.forEach { member ->
                                val memberPlayer = Bukkit.getPlayerExact(member)
                                memberPlayer?.sendMessage("${ChatColor.DARK_GREEN}$targetPlayerName${ChatColor.GREEN} has been kicked from the town by ${player.name}.")
                            }
                            targetPlayer.sendMessage("${ChatColor.RED}You have been kicked from the town by ${player.name}.")
                        } else {
                            sender.sendMessage("${ChatColor.RED}You don't have the authority to kick $targetPlayerName.")
                        }
                        val townMembersAfterLeave = townManager.getTownMembers(townName)
                        val baseChunks = config.getBaseChunks()
                        val chunksPerMember = config.getChunksPerMember()
                        val maxChunksPerTown = (baseChunks + chunksPerMember * townMembersAfterLeave.size).toInt()
                        townManager.setMaxChunksForTown(townName, maxChunksPerTown)
                    } else {
                        sender.sendMessage("${ChatColor.RED}$targetPlayerName is not a member of your town.")
                    }
                } else {
                    sender.sendMessage("${ChatColor.RED}Player $targetPlayerName is not online.")
                }
            } else {
                sender.sendMessage("${ChatColor.RED}You do not have permission to kick players from the town.")
            }
        } else {
            sender.sendMessage("${ChatColor.RED}You are not a member of any town.")
        }
    }
    private fun executeInfoCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /t info <townName>")
            return
        }
        val townName = args[1]
        val townInfo = townManager.getTownInfo(townName)
        val onlineMembers = getOnlineMembersInTown(townName)
        if (townInfo != null) {
            val maxChunksPerTown = townManager.getMaxChunksForTown(townName)
            sender.sendMessage("${ChatColor.DARK_BLUE}Town Information for ${ChatColor.AQUA}$townName:")
            sender.sendMessage("${ChatColor.BLUE}Mayor: ${ChatColor.AQUA}${townInfo.mayor}")
            sender.sendMessage("${ChatColor.BLUE}Members [${onlineMembers.size}]: ${ChatColor.AQUA}${townInfo.members.joinToString(", ")}")
            sender.sendMessage("${ChatColor.BLUE}Town Balance: ${ChatColor.AQUA}${townInfo.balance}")
            sender.sendMessage("${ChatColor.BLUE}Claimed Chunks: ${ChatColor.AQUA}${townInfo.claimedChunks.size}/$maxChunksPerTown")
            val description = townManager.getTownDescription(townName)
            if (description != null) {
                sender.sendMessage("${ChatColor.BLUE}Description: ${ChatColor.AQUA}$description")
            } else {
                sender.sendMessage("${ChatColor.BLUE}Description: ${ChatColor.AQUA}/t set description to set a town description!")
            }
            val ranks = townInfo.ranks.filterNot { it == TownRank.MAYOR || it == TownRank.MEMBER }
            ranks.forEach { rank ->
                val playersWithRanks = townManager.getPlayersWithRanks(townName, rank)
                if (playersWithRanks.isNotEmpty()) {
                    val playerList = playersWithRanks.flatMap { it.second }
                    sender.sendMessage("${ChatColor.BLUE}${rank.name.toLowerCase()}s: ${ChatColor.AQUA}${playerList.joinToString(", ")}")
                }
            }
        } else {
            sender.sendMessage("${ChatColor.RED}Town $townName not found.")
        }
    }
    private fun executeHelpCommand(sender: Player, page: Int) {
        val commandsPerPage = 6
        val commandList = listOf(
            "${ChatColor.BLUE}/t ${ChatColor.AQUA}new ${ChatColor.WHITE}<townName> - ${ChatColor.AQUA}Create a new town.",
            "${ChatColor.BLUE}/t ${ChatColor.AQUA}delete - ${ChatColor.AQUA}Delete your town (mayor only).",
            "${ChatColor.BLUE}/t ${ChatColor.AQUA}rank ${ChatColor.WHITE}<add/remove> <rank> <player> - ${ChatColor.AQUA}Manage player ranks.",
            "${ChatColor.BLUE}/t ${ChatColor.AQUA}invite ${ChatColor.WHITE}<player> - ${ChatColor.AQUA}Invite a player to your town.",
            "${ChatColor.BLUE}/t ${ChatColor.AQUA}accept ${ChatColor.WHITE}<townName> - ${ChatColor.AQUA}Accept a town invitation.",
            "${ChatColor.BLUE}/t ${ChatColor.AQUA}deny ${ChatColor.WHITE}<townName> - ${ChatColor.AQUA}Deny a town invitation.",
            "${ChatColor.BLUE}/t ${ChatColor.AQUA}leave - ${ChatColor.AQUA}Leave your current town.",
            "${ChatColor.BLUE}/t ${ChatColor.AQUA}help - ${ChatColor.AQUA}Show town commands.",
            "${ChatColor.BLUE}/t ${ChatColor.AQUA}deposit ${ChatColor.WHITE}<amount> - ${ChatColor.AQUA}Deposit money into town balance.",
            "${ChatColor.BLUE}/t ${ChatColor.AQUA}withdraw ${ChatColor.WHITE}<amount> - ${ChatColor.AQUA}Withdraw money from town balance.",
            "${ChatColor.BLUE}/t ${ChatColor.AQUA}claim - ${ChatColor.AQUA}Claim the chunk you're standing in for your town.",
            "${ChatColor.BLUE}/t ${ChatColor.AQUA}unclaim - ${ChatColor.AQUA}Unclaim the chunk you're standing in for your town.",
            "${ChatColor.BLUE}/t ${ChatColor.AQUA}info ${ChatColor.WHITE}<townName> - ${ChatColor.AQUA}Display information about a town.",
            "${ChatColor.BLUE}/t ${ChatColor.AQUA}ranklist ${ChatColor.WHITE}<townName> - ${ChatColor.AQUA}Show all players' ranks in a town.",
            "${ChatColor.BLUE}/t ${ChatColor.AQUA}reslist - ${ChatColor.AQUA}Show all residents in your town.",
        )
        val totalPages = Math.ceil(commandList.size / commandsPerPage.toDouble()).toInt()

        if (page < 1 || page > totalPages) {
            sender.sendMessage("${ChatColor.RED}Invalid page number. Available pages: 1-$totalPages")
            return
        }

        sender.sendMessage("${ChatColor.DARK_BLUE}Town Commands - Page $page:")
        val startIndex = (page - 1) * commandsPerPage
        val endIndex = minOf(startIndex + commandsPerPage, commandList.size)

        for (i in startIndex until endIndex) {
            sender.sendMessage(commandList[i])
        }

        val navigationText = TextComponent("${ChatColor.DARK_BLUE}[<<] Previous Page ${ChatColor.AQUA}| ")
        navigationText.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/t help ${page - 1}")

        val nextNavigationText = TextComponent("${ChatColor.DARK_BLUE}Next Page [>>]")
        nextNavigationText.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/t help ${page + 1}")
        sender.sendMessage(navigationText, nextNavigationText)
    }
    private fun executeLeaveCommand(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.DARK_RED}Only players can use this command.")
            return
        }

        val townName = townManager.getTownByPlayer(sender.name)

        if (townName != null) {
            val senderRank = townManager.getPlayerRank(sender.name, townName)

            if (senderRank == TownRank.MAYOR) {
                sender.sendMessage("${ChatColor.DARK_RED}Mayors cannot leave their own town. Use /town delete to disband the town.")
                return
            }

            if (sender.hasPermission(TownPermissions.LEAVE_TOWN.permission)) {
                val townMembersBeforeLeave = townManager.getTownMembers(townName)

                townManager.removePlayerFromTown(sender.name, townName)

                sender.sendMessage("${ChatColor.GREEN}You have left the town.")
                val townMembersAfterLeave = townManager.getTownMembers(townName)
                // updates the maximum claimed chunks after the player leaves
                val baseChunks = config.getBaseChunks()
                val chunksPerMember = config.getChunksPerMember()
                val maxChunksPerTown = (baseChunks + chunksPerMember * townMembersAfterLeave.size).toInt()
                townManager.setMaxChunksForTown(townName, maxChunksPerTown)
                townMembersBeforeLeave.forEach { member ->
                    if (!townMembersAfterLeave.contains(member)) {
                        val memberPlayer = Bukkit.getPlayerExact(member)
                        memberPlayer?.sendMessage("${ChatColor.DARK_GREEN}${sender.name}${ChatColor.GREEN} has left the town D: (sadge)!")
                    }
                }

            } else {
                sender.sendMessage("${ChatColor.DARK_RED}You do not have permission to leave the town.")
            }
        } else {
            sender.sendMessage("${ChatColor.DARK_RED}You are not a member of any town.")
        }
    }
    private fun executeNewTownCommand(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.DARK_RED}Only players can use this command.")
            return
        }
        if (args.size < 2) {
            sender.sendMessage(TownCreateMessages.INVALID_USAGE.message)
            return
        }
        val townName = args[1]
        if (!isValidTownName(townName)) {
            sender.sendMessage("${ChatColor.RED}Invalid town name. Town name can only contain letters (a-z, A-Z), numbers (0-9), and underscore (_).")
            return
        }
        if (townManager.isTownExists(townName)) {
            sender.sendMessage(TownCreateMessages.ALREADY_EXISTS.message.format(townName))
            return
        }

        val playerName = sender.name

        if (townManager.isPlayerInTown(playerName)) {
            val currentTown = townManager.getTownByPlayer(playerName)
            sender.sendMessage(TownCreateMessages.ALREADY_MEMBER.message.format(currentTown))
            return
        }
        val chunk = sender.location.chunk
        if (townManager.isChunkClaimed(chunk)) {
            sender.sendMessage("${ChatColor.RED}Chunk you are in is already claimed by another town")
            return
        }
        val townCreateCost = config.getTownCreateCost()
        val economy = plugin.getEconomy()

        if (economy != null) {
            val playerBalance = economy.getBalance(sender)

            if (economy.has(sender, townCreateCost)) {
                economy.withdrawPlayer(sender, townCreateCost)

                sender.sendMessage("${ChatColor.GREEN}Successfully created town! $townCreateCost dollars has been taken out of your bank.")
            } else {
                sender.sendMessage("${ChatColor.RED}You need at least $townCreateCost to create a town. Your current balance: $playerBalance")
                return
            }
        } else {
            sender.sendMessage("${ChatColor.RED}Economy plugin not found. Unable to create town.")
            return
        }
        val mayor = sender.name
        val members = mutableListOf(mayor)

        townManager.createTown(townName, mayor, members)
        if (townManager.claimChunk(townName, chunk)) {
            townManager.markChunkAsUnclaimable(townName, chunk)
        }

        val baseChunks = config.getBaseChunks()
        val chunksPerMember = config.getChunksPerMember()
        val townMembers = townManager.getTownMembers(townName).size
        val maxChunks = (baseChunks + chunksPerMember * townMembers).toInt()
        townManager.setMaxChunksForTown(townName, maxChunks)
        config.assignRankPermissions(playerName, TownRank.MAYOR)
        townManager.addPlayerToRank(mayor, townName, TownRank.MAYOR)
        townManager.addPlayerToRank(mayor, townName, TownRank.MEMBER)
        Bukkit.broadcastMessage(TownCreateMessages.SUCCESSFUL.message.format(townName))
    }
    private fun isValidTownName(townName: String): Boolean {
        val regex = Regex("^[a-zA-Z0-9_]+$")
        return townName.matches(regex)
    }
    private fun executeDeleteTownCommand(sender: CommandSender) {
        val playerName = sender.name
        val townName = townManager.getTownByMayor(playerName)

        if (townName != null) {
                if (sender.hasPermission(TownPermissions.DELETE_TOWN.permission)) {
                    val members = townManager.getTownMembers(townName)

                    townManager.deleteTown(townName)
                    Bukkit.broadcastMessage(TownDeleteMessages.SUCCESSFUL.message.format(townName))

                    members.forEach { member ->
                        config.assignRankPermissions(member, TownRank.MEMBER)
                    }
                } else {
                    sender.sendMessage(TownDeleteMessages.NO_PERMISSION.message)
                }
            } else {
                sender.sendMessage(TownDeleteMessages.NO_PERMISSION.message)
            }
        }
    private fun executeRankCommand(sender: CommandSender, args: Array<out String>) {
        val townName = townManager.getTownByPlayer(sender.name)
        if (townName != null) {
            if (args.size < 4) {
                if (args.size == 3) {
                    val ranks = TownRank.values().filter { it != TownRank.MEMBER && it != TownRank.MAYOR }
                        .sortedByDescending { it.priority }
                    sender.sendMessage("Available ranks: ${ranks.joinToString(", ") { it.name }}")
                    return
                } else {
                    sender.sendMessage(TownRankMessages.INVALID_USAGE.message)
                    return
                }
            }

            val targetPlayerName = args[3]
            val targetRankName = args[2].uppercase(Locale.getDefault())
            val action = args[1].lowercase(Locale.getDefault())
            if (sender.hasPermission(TownPermissions.MANAGE_RANKS.permission)) {
                if (townManager.isPlayerInTown(targetPlayerName)) {
                    val targetRank = try {
                        TownRank.valueOf(targetRankName)
                    } catch (e: IllegalArgumentException) {
                        sender.sendMessage(TownRankMessages.INVALID_USAGE.message)
                        return
                    }

                    val senderRank = townManager.getPlayerRank(sender.name, townName)
                    // for error logging if sql ever does a goofy:
                    // println("Sender Rank: $senderRank, Target Rank: $targetRank")

                    if (senderRank != null && senderRank.priority >= targetRank.priority) {
                    val targetPlayer = Bukkit.getPlayerExact(targetPlayerName)
                        if (targetPlayer != null) {
                            when (action) {
                                "add" -> {
                                    if (!townManager.hasPlayerRank(targetPlayerName, townName, targetRank)) {
                                        val existingRank = townManager.getPlayerRank(targetPlayerName, townName)
                                        val existingPermissions = if (existingRank != null) {
                                            config.getRankPermissions(existingRank)
                                        } else {
                                            emptyList()
                                        }

                                        townManager.addPlayerToRank(targetPlayerName, townName, targetRank)

                                        // Combine existing and new permissions
                                        val newPermissions = existingPermissions + config.getRankPermissions(
                                            targetRank
                                        )
                                        config.assignRankPermissions(targetPlayerName, newPermissions)
                                        val townMembers = townManager.getTownMembers(townName)
                                        townMembers.forEach { member ->
                                            val memberPlayer = Bukkit.getPlayerExact(member)
                                            memberPlayer?.sendMessage("${ChatColor.DARK_GREEN}$targetPlayerName${ChatColor.GREEN} has been promoted to $targetRank in $townName!")
                                        }
                                    } else {
                                        sender.sendMessage(TownRankMessages.ALREADY_HAS_RANK.message.format(targetPlayerName, targetRank))
                                    }
                                }
                                "remove" -> {
                                    if (senderRank.priority > targetRank.priority) {
                                        if (townManager.hasPlayerRank(targetPlayerName, townName, targetRank)) {
                                            // checks if the target rank isnt member or mayor (so you cant remove them bc uh obviously)
                                            if (targetRank != TownRank.MEMBER && targetRank != TownRank.MAYOR) {
                                                val currentRank = townManager.getPlayerRank(targetPlayerName, townName)

                                                townManager.removePlayerFromRank(targetPlayerName, townName, targetRank)
                                                val townMembers = townManager.getTownMembers(townName)
                                                townMembers.forEach { member ->
                                                    val memberPlayer = Bukkit.getPlayerExact(member)
                                                    memberPlayer?.sendMessage("${ChatColor.DARK_GREEN}$targetPlayerName${ChatColor.GREEN} has been demoted from $targetRank in $townName!")
                                                }
                                                if (currentRank != null) {
                                                    val ranks = TownRank.values().filter { it != TownRank.MEMBER && it != TownRank.MAYOR }
                                                        .sortedByDescending { it.priority }

                                                    val secondToHighestRank = ranks.getOrNull(1)

                                                    if (secondToHighestRank != null) {
                                                        val permissions = config.getRankPermissions(
                                                            secondToHighestRank
                                                        )
                                                        config.assignRankPermissions(targetPlayerName, permissions)
                                                    } else {
                                                        sender.sendMessage("Error: Unable to find second-to-highest rank.")
                                                    }
                                                } else {
                                                    sender.sendMessage("Error: Unable to determine current rank for the player.")
                                                }
                                            } else {
                                                sender.sendMessage(TownRankMessages.INVALID_USAGE.message)
                                            }
                                        } else {
                                            sender.sendMessage(TownRankMessages.DOES_NOT_HAVE_RANK.message.format(targetPlayerName, targetRank))
                                        }
                                    } else {
                                        sender.sendMessage(TownRankMessages.NO_PERMISSION.message)
                                    }
                                }
                                else -> {
                                    sender.sendMessage(TownRankMessages.INVALID_USAGE.message)
                                }
                            }
                        } else {
                            sender.sendMessage(TownRankMessages.PLAYER_NOT_FOUND.message.format(targetPlayerName))
                        }
                    } else {
                        sender.sendMessage(TownRankMessages.NO_PERMISSION.message)
                    }
                } else {
                    sender.sendMessage(TownRankMessages.NOT_MEMBER.message.format(targetPlayerName))
                }
            } else {
                sender.sendMessage(TownRankMessages.NO_PERMISSION.message)
            }
        } else {
            sender.sendMessage(TownRankMessages.NOT_MEMBER.message.format(sender.name))
        }
    }
    private fun executeInviteCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /t invite <player>")
            return
        }
        val invitedPlayerName = args[1]
        val player = sender as? Player ?: return

        val townName = townManager.getTownByPlayer(player.name)

        if (townName != null) {
            if (sender.hasPermission(TownPermissions.TOWN_INVITE_PLAYER.permission)) {
                val invitedPlayer = Bukkit.getPlayerExact(invitedPlayerName)

                if (invitedPlayer != null) {
                    val invitedPlayerTown = townManager.getTownByPlayer(invitedPlayerName)

                    if (invitedPlayerTown == null) {
                        if (!townManager.isPlayerInvited(invitedPlayerName, townName)) {
                            val success = townManager.storeInvitation(player.name, invitedPlayerName, townName)

                            if (success) {
                                val confirmText = TextComponent("${ChatColor.GREEN}[Confirm]")
                                confirmText.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/t accept $townName")

                                val denyText = TextComponent("${ChatColor.RED}[Deny]")
                                denyText.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/t deny $townName")
                                val invitationText = TextComponent("${ChatColor.GREEN}Invitation sent to $invitedPlayerName. They can: ")
                                invitationText.addExtra(confirmText)
                                invitationText.addExtra("${ChatColor.GREEN} or ")
                                invitationText.addExtra(denyText)
                                sender.sendMessage(invitationText)
                                val invitedPlayerMessage = TextComponent("${ChatColor.GREEN}You have been invited to join the town $townName. You can: ")
                                invitedPlayerMessage.addExtra(confirmText)
                                invitedPlayerMessage.addExtra("${ChatColor.GREEN} or ")
                                invitedPlayerMessage.addExtra(denyText)
                                invitedPlayer.sendMessage(invitedPlayerMessage)
                            } else {
                                sender.sendMessage("${ChatColor.RED}An error occurred while sending the invitation.")
                            }
                        } else {
                            sender.sendMessage("${ChatColor.RED}$invitedPlayerName has already been invited to join the town.")
                        }
                    } else {
                        sender.sendMessage("${ChatColor.RED}$invitedPlayerName is already a member of a town.")
                    }
                } else {
                    sender.sendMessage("${ChatColor.RED}Player $invitedPlayerName is not online.")
                }
            } else {
                sender.sendMessage("${ChatColor.RED}You do not have permission to invite players to the town.")
            }
        } else {
            sender.sendMessage("${ChatColor.RED}You are not a member of any town.")
        }
    }
    private fun executeAcceptCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(TownInvitationMessages.ACCEPT_USAGE.message)
            return
        }

        val townName = args[1]
        val player = sender as Player

        val success = townManager.acceptInvitation(player.name, townName)
        val townMembers = townManager.getTownMembers(townName)

        if (success) {
            config.assignRankPermissions(player.name, TownRank.MEMBER)
            townManager.addPlayerToRank(player.name, townName, TownRank.MEMBER)

            // Calculate max chunks based on the formula
            val baseChunks = config.getBaseChunks()
            val chunksPerMember = config.getChunksPerMember()
            val townMembersCount = townMembers.size
            val maxChunks = (baseChunks + chunksPerMember * townMembersCount).toInt()
            townManager.setMaxChunksForTown(townName, maxChunks)

            townMembers.forEach { member ->
                Bukkit.getPlayerExact(member)?.sendMessage("${ChatColor.DARK_GREEN}${player.name}${ChatColor.GREEN} has joined the town.")
            }
            sender.sendMessage(TownInvitationMessages.JOIN_SUCCESSFUL.message.format(townName))

        } else {
            sender.sendMessage(TownInvitationMessages.JOIN_FAILURE_FORMAT.message.format(townName))
        }
    }
    private fun executeDenyCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(TownInvitationMessages.DECLINE_USAGE.message)
            return
        }

        val townName = args[1]
        val player = sender as Player

        val success = townManager.declineInvitation(player.name, townName)
        val townMembers = townManager.getTownMembers(townName)
        if (success) {
            sender.sendMessage(TownInvitationMessages.DECLINE_SUCCESSFUL.message.format(townName))
            townMembers.forEach { member ->
                Bukkit.getPlayerExact(member)?.sendMessage("${ChatColor.DARK_GREEN}${player.name}${ChatColor.GREEN} has denied the invitation to join your town.")
            }
        } else {
            sender.sendMessage(TownInvitationMessages.DECLINE_FAILURE.message.format(townName))
        }
    }
    private fun executeDepositCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /t deposit <amount>")
            return
        }
        if (!sender.hasPermission(TownPermissions.TOWN_DEPOSIT.permission)) {
            sender.sendMessage("${ChatColor.DARK_RED}You do not have permission to perform this action.")
            return
        }

        val player = sender as Player
        val playerName = player.name
        val townName = townManager.getTownByPlayer(playerName)

        if (townName == null) {
            sender.sendMessage("${ChatColor.RED}You are not a member of any town.")
            return
        }

        val amountStr = args[1]

        // checks if the amount is a whole number
        val amount: Double = try {
            val parsedAmount = amountStr.toDouble()
            if (parsedAmount % 1 != 0.0 || parsedAmount < 0) {
                throw NumberFormatException()
            }
            parsedAmount
        } catch (e: NumberFormatException) {
            sender.sendMessage("${ChatColor.RED}Invalid amount format. Please enter a non-negative whole number.")
            return
        }

        val economy = plugin.getEconomy()

        if (economy != null) {
            val personalBalance = economy.getBalance(player)

            if (personalBalance < amount) {
                sender.sendMessage("${ChatColor.RED}Insufficient funds. You don't have enough money in your personal account.")
                return
            }

            if (economy.withdrawPlayer(playerName, amount).transactionSuccess()) {
                val currentBalance = townManager.getTownBalance(townName)
                val newBalance = currentBalance + amount

                townManager.updateTownBalance(townName, newBalance)

                sender.sendMessage("${ChatColor.GREEN}Deposited $amount to town $townName. New town balance: $newBalance")
            } else {
                sender.sendMessage("${ChatColor.RED}Failed to deposit funds. Please check your balance.")
            }
        } else {
            sender.sendMessage("${ChatColor.RED}Vault economy not found. Please check your server setup.")
        }
    }
    private fun executeWithdrawCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /t withdraw <amount>")
            return
        }

        if (!sender.hasPermission(TownPermissions.TOWN_WITHDRAW.permission)) {
            sender.sendMessage("${ChatColor.DARK_RED}You do not have permission to perform this action.")
            return
        }

        if (sender !is Player) {
            sender.sendMessage("${ChatColor.DARK_RED}This command can only be executed by players.")
            return
        }

        val playerName = sender.name
        val townName = townManager.getTownByPlayer(playerName)

        if (townName == null) {
            sender.sendMessage("${ChatColor.RED}You are not a member of any town.")
            return
        }

        val amountStr = args[1]

        val amount: Double = try {
            val parsedAmount = amountStr.toDouble()
            if (parsedAmount % 1 != 0.0 || parsedAmount < 0) {
                throw NumberFormatException()
            }
            parsedAmount
        } catch (e: NumberFormatException) {
            sender.sendMessage("${ChatColor.RED}Invalid amount format. Please enter a non-negative whole number.")
            return
        }

        val currentBalance = townManager.getTownBalance(townName)

        if (currentBalance < amount) {
            sender.sendMessage("${ChatColor.RED}Insufficient funds in town $townName.")
            return
        }

        val economy = plugin.getEconomy()

        if (economy != null) {
            val depositResult = economy.depositPlayer(playerName, amount)

            if (depositResult.transactionSuccess()) {
                val newBalance = currentBalance - amount
                townManager.updateTownBalance(townName, newBalance)

                sender.sendMessage("${ChatColor.GREEN}Withdrawn $amount from town $townName. New town balance: $newBalance")
            } else {
                sender.sendMessage("${ChatColor.RED}Failed to withdraw funds. Please check your balance.")
            }
        } else {
            sender.sendMessage("${ChatColor.RED}Vault economy not found. Please check your server setup.")
        }
    }
    private fun executeClaimCommand(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(TownMessages.NO_PERMISSION.message)
            return
        }

        val townName = townManager.getTownByPlayer(sender.name)

        if (!sender.hasPermission(TownPermissions.TOWN_CHUNK_CLAIM.permission)) {
            sender.sendMessage(TownMessages.NO_PERMISSION.message)
            return
        }

        if (townName == null) {
            sender.sendMessage(TownMessages.NOT_IN_TOWN.message)
            return
        }

        val townBalance = townManager.getTownBalance(townName)
        val chunkClaimCost = config.getTownChunkCost()

        if (townBalance <= chunkClaimCost) {
            sender.sendMessage("${ChatColor.RED}You don't have $chunkClaimCost in your town bank to claim a chunk, you only have $townBalance in your town.")
            return
        }

        val chunk = sender.location.chunk
        val adjacentClaimed = townManager.getAdjacentClaimedChunks(townName, chunk)

        if (adjacentClaimed.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}You can only claim a chunk if it's located right next to another claimed chunk.")
            return
        }

        if (townManager.isChunkClaimedByTown(townName, chunk)) {
            sender.sendMessage(TownClaimMessages.CHUNK_IS_CLAIMED.message)
            return
        }
        if (townManager.isChunkClaimed(chunk)) {
            sender.sendMessage(TownClaimMessages.CHUNK_IS_CLAIMED.message)
            return
        }
        val maxChunksPerTown = townManager.getMaxChunksForTown(townName)
        val claimedChunks = townManager.getTownClaimedChunks(townName)
        if (claimedChunks.size >= maxChunksPerTown) {
            sender.sendMessage("${ChatColor.RED}Your town has reached the maximum allowed chunks.")
            return
        }

        val newTownBalance = townBalance - chunkClaimCost

        if (townManager.claimChunk(townName, chunk)) {
            sender.sendMessage(TownClaimMessages.CLAIM_SUCCESS.message.format(chunk.x, chunk.z, townName))
            townManager.updateTownBalance(townName, newTownBalance)
        }
    }
    private fun executeUnclaimCommand(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(TownMessages.NO_PERMISSION.message)
            return
        }

        if (!sender.hasPermission(TownPermissions.TOWN_CHUNK_UNCLAIM.permission)) {
            sender.sendMessage(TownMessages.NO_PERMISSION.message)
            return
        }
        val townName = townManager.getTownByPlayer(sender.name)

        if (townName == null) {
            sender.sendMessage(TownMessages.NOT_IN_TOWN.message)
            return
        }

        val chunk = sender.location.chunk

        if (!townManager.isChunkClaimedByTown(townName, chunk)) {
            sender.sendMessage(TownClaimMessages.CHUNK_NOT_CLAIMED_BY_TOWN.message)
            return
        }

        if (townManager.isChunkUnclaimable(townName, chunk)) {
            sender.sendMessage(TownClaimMessages.CHUNK_UNCLAIMABLE.message)
            return
        }

        if (townManager.unclaimChunk(townName, chunk)) {
            sender.sendMessage(TownClaimMessages.UNCLAIM_SUCCESS.message.format(chunk.x, chunk.z, townName))
        } else {
            sender.sendMessage(TownClaimMessages.UNCLAIM_FAILURE.message)
        }
    }
    private fun executeOnlineCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /t online <townName>")
            return
        }

        val townName = args[1]
        if (!townManager.isTownExists(townName)) {
            sender.sendMessage("${ChatColor.RED}The town $townName does not exist.")
            return
        }

        val onlineMembers = getOnlineMembersInTown(townName)

        if (onlineMembers.isNotEmpty()) {
            sender.sendMessage("${ChatColor.DARK_BLUE}Online Members in $townName [${onlineMembers.size}]:")
            sender.sendMessage("${ChatColor.AQUA}${onlineMembers.joinToString(", ")}")
        } else {
            sender.sendMessage("${ChatColor.DARK_BLUE}No online members in $townName.")
        }
    }
    private fun getOnlineMembersInTown(townName: String): List<String> {
        val allMembers = townManager.getTownMembers(townName)
        val onlinePlayers = Bukkit.getOnlinePlayers().map { it.name }

        return allMembers.filter { onlinePlayers.contains(it) }
    }
    private fun executeSetCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}Usage: /town set <subcommand>")
            return
        }

        val subCommand = args[1].lowercase(Locale.getDefault())

        when (subCommand) {
            "mayor" -> executeSetMayorCommand(sender, args)
            "name" -> executeSetNameCommand(sender, args)
            "description" -> executeSetDescriptionCommand(sender, args)
            else -> {
                sender.sendMessage("${ChatColor.RED}Unknown subcommand: $subCommand")
                sender.sendMessage("${ChatColor.RED}Usage: /town set <subcommand>")
            }
        }
    }
    private fun executeSetMayorCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("${ChatColor.RED}Usage: /town set mayor <player>")
            return
        }

        val playerName = args[2]
        val townName = townManager.getTownByPlayer(sender.name)

        if (townName == null) {
            sender.sendMessage("${ChatColor.RED}You are not a member of any town.")
            return
        }

        val currentMayor = townManager.getTownMayor(townName)

        if (sender.name != currentMayor) {
            sender.sendMessage("${ChatColor.RED}Only the current mayor can set a new mayor.")
            return
        }

        if (townManager.getTownByPlayer(playerName) == null) {
            sender.sendMessage("${ChatColor.RED}$playerName is not a member of your town.")
            return
        }

        if (playerName.equals(currentMayor, ignoreCase = true)) {
            sender.sendMessage("${ChatColor.RED}$playerName is already the mayor.")
            return
        }

        val targetPlayer = Bukkit.getPlayerExact(playerName)

        if (targetPlayer == null) {
            sender.sendMessage("${ChatColor.RED}Player $playerName not found.")
            return
        }

        // Set the new mayor and give them the mayor rank
        townManager.setTownMayor(townName, playerName)
        config.assignRankPermissions(sender.name, TownRank.MEMBER)
        config.assignRankPermissions(playerName, TownRank.MAYOR)
        townManager.addPlayerToRank(playerName, townName, TownRank.MAYOR)
        townManager.removePlayerFromRank(sender.name, townName, TownRank.MAYOR)
        sender.sendMessage("${ChatColor.GREEN}$playerName has been set as the new mayor of the town.")
        targetPlayer.sendMessage("${ChatColor.GREEN}You are now the mayor of the town.")
    }
    private fun executeSetNameCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("${ChatColor.RED}Usage: /town set name <newName>")
            return
        }
        if (!sender.hasPermission(TownPermissions.TOWN_SET_NAME.permission)) {
            sender.sendMessage(TownMessages.NO_PERMISSION.message)
            return
        }
        val newName = args[2]
        if (townManager.isTownExists(newName)) {
            sender.sendMessage("${ChatColor.RED}The name '$newName' is already taken.")
            return
        }
        townManager.setTownName(sender.name, newName)
        sender.sendMessage("${ChatColor.GREEN}Town name updated to: $newName.")
    }
    private fun executeSetDescriptionCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("${ChatColor.RED}Usage: /town set description <description>")
            return
        }
        if (!sender.hasPermission(TownPermissions.TOWN_SET_DESCRIPTION.permission)) {
            sender.sendMessage(TownMessages.NO_PERMISSION.message)
            return
        }
        val description = args.sliceArray(2 until args.size).joinToString(" ")
        val townName = townManager.getTownByPlayer(sender.name)

        if (townName == null) {
            sender.sendMessage("${ChatColor.RED}You are not a member of any town.")
            return
        }

        if (sender.name != townManager.getTownMayor(townName)) {
            sender.sendMessage("${ChatColor.RED}Only the mayor can set the town description.")
            return
        }
        townManager.setTownDescription(townName, description)
        sender.sendMessage("${ChatColor.GREEN}Town description updated successfully.")
    }
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return SUB_COMMANDS.filter { it.startsWith(args[0], ignoreCase = true) }
        } else if (args.size == 2 && args[0].equals("set", ignoreCase = true)) {
            return SET_SUB_COMMANDS.filter { it.startsWith(args[1], ignoreCase = true) }
        } else if (args.size == 3 && args[1].equals("mayor", ignoreCase = true)) {
            return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }
        } else if (args.size == 2 && args[0].equals("rank", ignoreCase = true)) {
            return listOf("add", "remove").filter { it.startsWith(args[1], ignoreCase = true) }
        } else if (args.size == 3 && args[0].equals("rank", ignoreCase = true) && (args[1] == "add" || args[1] == "remove")) {
            return TownRank.entries.filter { it != TownRank.MAYOR && it != TownRank.MEMBER }.map { it.name.lowercase() }.filter { it.startsWith(args[2], ignoreCase = true) }
        } else if ((args.size == 2) && args [0].equals("list", ignoreCase = true)) {
            return listOf("by")
        } else if ((args.size == 3) && args[0].equals("list", ignoreCase = true) && args[1].equals("by", ignoreCase = true)) {
            return listOf("chunks", "members", "wealth").filter { it.startsWith(args[args.size - 1], ignoreCase = true) }
        } else if (args.size == 4 && args[0].equals("rank", ignoreCase = true) && (args[1] == "add" || args[1] == "remove")) {
            return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[3], ignoreCase = true) }
        } else if (args.size == 2 && args[0].equals("invite", ignoreCase = true) || args[0].equals("kick", ignoreCase = true)) {
            return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
        } else if (args.size == 2 && args[0].equals("info", ignoreCase = true) || args[0].equals("online", ignoreCase = true) || args[0].equals("ranklist", ignoreCase = true) || args[0].equals("reslist", ignoreCase = true) || args[0].equals("gui", ignoreCase = true)) {
        return townManager.getAllTowns().filter { it.startsWith(args[1], ignoreCase = true) }
    }
        return emptyList()
    }
}