// ISSUE: KT-76529, KT-90035
// FILE: One.java
public class One {

	public static class StaticHolder {
        static int counter = 5;
    }
}

// FILE: main.kt

import kotlin.test.*

fun box(): String {
    // Should be compilable (no invisible reference)
    val counter: Int = One.StaticHolder.counter
    assertEquals(5, counter)
    return "OK"
}

// FILE: lombok.config
lombok.fieldDefaults.defaultPrivate = true
