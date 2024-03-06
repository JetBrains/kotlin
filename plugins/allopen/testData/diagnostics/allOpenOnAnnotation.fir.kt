// WITH_STDLIB
// ISSUE: KT-63507

annotation class AllOpen

@AllOpen
annotation class SubComponent(
    val <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>scope<!>: String
)
