package site.albaniacraft.towns.towns
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.plugin.java.JavaPlugin
import site.albaniacraft.towns.constants.TownRank
import site.albaniacraft.towns.utils.Config
import java.time.LocalTime
import java.time.format.DateTimeParseException
import java.util.*

class UpkeepManager(private val plugin: JavaPlugin) {
    private val townManager = TownManager(plugin)
    private val timer = Timer()
    private val config = Config(plugin)
    // never tested this out gonna assume it works lol
    fun scheduleUpkeep() {
        val config = plugin.config
        if (!config.getBoolean("upkeep.enabled")) {
            return
        }

        val timeString = config.getString("townupkeep.time") ?: "12:00"
        val timeZoneString = config.getString("townupkeep.timeZone") ?: "America/New_York"
        val amount = config.getDouble("townupkeep.amount", 100.0)

        val zoneId = TimeZone.getTimeZone(timeZoneString).toZoneId()

        val upkeepTime = try {
            LocalTime.parse(timeString)
        } catch (e: DateTimeParseException) {
            plugin.logger.warning("Failed to parse upkeep time. Using default time (12:00).")
            LocalTime.parse("12:00")
        }

        val now = Calendar.getInstance(TimeZone.getTimeZone(zoneId))
        val nextUpkeepDateTime = now.clone() as Calendar
        nextUpkeepDateTime.set(Calendar.HOUR_OF_DAY, upkeepTime.hour)
        nextUpkeepDateTime.set(Calendar.MINUTE, upkeepTime.minute)
        val delayMillis = nextUpkeepDateTime.timeInMillis - now.timeInMillis

        plugin.logger.info("Scheduled upkeep task. Time: ${nextUpkeepDateTime.time}, Delay (milliseconds): $delayMillis")
        timer.schedule(object : TimerTask() {
            override fun run() {
                plugin.logger.info("Executing upkeep task at ${Calendar.getInstance(TimeZone.getTimeZone(zoneId)).time}")
                performUpkeep(amount)
            }
        }, delayMillis)
    }

    private fun performUpkeep(amount: Double) {
        Bukkit.broadcastMessage("${ChatColor.GREEN}The daily upkeep has commenced gl \uD83D\uDE4F.")
        for (townName in townManager.getAllTowns()) {
            val townBalance = townManager.getTownBalance(townName)
            if (townBalance < amount) {
                val members = townManager.getTownMembers(townName)
                townManager.deleteTown(townName)
                members.forEach { member ->
                    config.assignRankPermissions(member, TownRank.MEMBER)
                }
                Bukkit.broadcastMessage("${ChatColor.RED}Town $townName has been deleted due to insufficient funds during upkeep.")
            } else {
                townManager.updateTownBalance(townName, townBalance - amount)
            }
        }
    }
}
