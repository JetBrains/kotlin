// ISSUE: KT-90387
// FILE: ChainFalseProbe.java

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true, chain = false)
public class ChainFalseProbe {
    private String name;
}

// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    val probe = ChainFalseProbe()
    probe.name("x")
    assertEquals("x", probe.name())
    return "OK"
}
