/*
 * Regression test for https://github.com/JetBrains/kotlin/pull/5466
 */
import org.jetbrains.kotlin.plugin.sandbox.AddSupertype

interface Target {
    fun value(): String
}

@AddSupertype(Target::class)
interface MergePoint {
    // Matches Target.value, requires an override modifier
    fun value(): String
}

fun box(): String {
    return "OK"
}