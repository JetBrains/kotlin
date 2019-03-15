// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1618

// Simplified example from https://youtrack.jetbrains.com/issue/KT-26787

enum class Role { PRIMARY, EXTRA }

class Location(val role: Role, val building: Int = 0)

fun box() : String {
    val result = mutableListOf<Location>()

    val props = arrayOf(
        Location(Role.PRIMARY),
        Location(Role.EXTRA),
        Location(Role.PRIMARY)
    )

    var loopCount = 0
    for (possiblyOutdated in props) {
        when (possiblyOutdated.role) {
            Role.PRIMARY -> {
                val index = result.indexOfFirst { it.building == possiblyOutdated.building }
            }
            Role.EXTRA -> {
            }
        }

        loopCount++
    }

    if (loopCount != 3) return "Wrong loop count $loopCount"

    return "OK"
}
