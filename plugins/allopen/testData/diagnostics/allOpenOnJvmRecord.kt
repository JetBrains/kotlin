// WITH_STDLIB
// JDK_KIND: FULL_JDK_17
// JVM_TARGET: 17
// ISSUE: KT-86286
// FIR_DUMP

annotation class AllOpen

@AllOpen
@JvmRecord
data class Some(val x: Int)
