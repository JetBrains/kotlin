// WITH_STDLIB

annotation class AllOpen

@AllOpen
annotation class ConsoleCommands(
    val <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>value<!>: String = "",
    val <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>scope<!>: String
)
