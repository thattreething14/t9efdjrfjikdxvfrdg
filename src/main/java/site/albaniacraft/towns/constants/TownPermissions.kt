package site.albaniacraft.towns.constants
//not to be confused with other one that handles command perms
enum class TownPermissions {
    INTERACT,
    BUILD,
    DESTROY,
    USE_CHESTS,
    HURT_ANIMALS,
    USE_ITEMS;

    companion object {
        val values: Array<TownPermissions> = entries.toTypedArray()
    }
}