// FIR_IDENTICAL
// WITH_STDLIB

annotation class AllOpen

@AllOpen
annotation class ConsoleCommands(
    <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>val value: String = ""<!>,
    <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>val scope: String<!>
)
