// FULL_JDK
// ISSUE: KT-89371

import lombok.Builder
import lombok.NoArgsConstructor
import lombok.extern.java.Log
import lombok.AccessLevel
import kotlin.test.assertEquals

@Builder
@NoArgsConstructor
class Built(var x: Int)

@Log(access = AccessLevel.PUBLIC)
@NoArgsConstructor
class Logged(var x: Int)

// And for one the class declares itself.
@NoArgsConstructor
class WithOwnCompanion(var x: Int) {
    companion object {
        val marker: String = "marker"
    }
}

fun box(): String {
    assertEquals(42, Built.builder().x(42).build().x)
    assertEquals(0, Built().x)

    assertEquals("Logged", Logged.log.name)
    assertEquals(0, Logged().x)

    assertEquals("marker", WithOwnCompanion.marker)
    assertEquals(0, WithOwnCompanion().x)

    return "OK"
}
