// FULL_JDK
// ISSUE: KT-86915

import lombok.extern.java.Log
import lombok.AccessLevel
import lombok.NoArgsConstructor
import kotlin.test.assertEquals

// `@Log` needs a companion object for its `log`, and `@NoArgsConstructor(staticName = ...)` for its `make`
// factory. A class has one companion object, so both features share the single generated one, and everything both
// of them put there has to work.
@Log(access = AccessLevel.PUBLIC)
@NoArgsConstructor(staticName = "make", force = true)
class Klass(val a: Int, val b: String)

fun box(): String {
    // The logger is the one for `Klass`, not for the companion object holding it
    assertEquals("Klass", Klass.log.name)
    Klass.log.info("Check `Klass.Companion.log`")

    // `make` lives in the very same companion object as `log`
    val klass = Klass.make()
    assertEquals(0, klass.a)

    return "OK"
}
