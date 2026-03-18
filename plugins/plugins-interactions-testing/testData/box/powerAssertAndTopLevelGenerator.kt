// ISSUE: KT-83931

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction

@DummyFunction("first.kt")
class First

fun box(): String = "OK"
