// KT-8375: @JvmName on a function does NOT propagate to the names of
// auto-generated classes (lambdas / anonymous objects) it creates.
//
// YouTrack: https://youtrack.jetbrains.com/issue/KT-8375
//
// Case 1 — class-lambda via @JvmSerializableLambda:
//   @JvmName("jvmName") fun kotlinName() = @JvmSerializableLambda {}
//   expected class:  _1Kt$jvmName$1
//   actual class:    _1Kt$kotlinName$1   (Kotlin source name used)
//
// Case 2 — plain indy lambda:
//   @JvmName("jvmName2") fun kotlinName2() = {}
//   expected method: jvmName2$lambda$0
//   actual method:   kotlinName2$lambda$0   (Kotlin source name used)

@file:JvmName("_1Kt")

import kotlin.jvm.JvmSerializableLambda

@JvmName("jvmName")
fun kotlinName() = @JvmSerializableLambda {}

@JvmName("jvmName2")
fun kotlinName2() = {}
