// ISSUE: KT-89024
// FIR_DUMP

import lombok.Builder
import kotlin.test.assertEquals

// An empty `builderMethodName` suppresses `builder()`, leaving `toBuilder()` as the only way into the
// builder - which is what the annotation is written for, spawning copies of an existing instance.
@Builder(toBuilder = true, builderMethodName = "")
class Suppressed(val x: Int)

// The builder class, its setters and `build()` are generated regardless, and so is the companion object
// of a class that still has a `builder()` to host.
@Builder(builderMethodName = "createBuilder")
class Renamed(val x: Int)

fun box(): String {
    val copy = Suppressed(1).toBuilder().build()
    assertEquals(1, copy.x)

    val built = Suppressed(2).toBuilder().x(3).build()
    assertEquals(3, built.x)

    assertEquals(4, Renamed.createBuilder().x(4).build().x)

    return "OK"
}
