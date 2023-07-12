// WITH_STDLIB

annotation class AllOpen

@AllOpen
annotation class ConsoleCommands(
    val value: String = "",
    val scope: String
)
