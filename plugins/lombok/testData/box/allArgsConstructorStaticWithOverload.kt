// FILE: StaticWithOverload.java

import lombok.AllArgsConstructor;

// The declared `of(int)` takes as many parameters as the factory, but of another type,
// so Lombok generates `of(String)` next to it
@AllArgsConstructor(staticName = "of")
public class StaticWithOverload {
    public String field;

    public static StaticWithOverload of(int value) {
        return new StaticWithOverload("explicit");
    }

    static void test() {
        if (!"explicit".equals(StaticWithOverload.of(1).field)) throw new AssertionError();
        if (!"generated".equals(StaticWithOverload.of("generated").field)) throw new AssertionError();
    }
}

// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    StaticWithOverload.test()
    assertEquals("explicit", StaticWithOverload.of(1).field)
    assertEquals("generated", StaticWithOverload.of("generated").field)
    return "OK"
}
