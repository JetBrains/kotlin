// WITH_STDLIB
// ISSUE: KT-54020
// FILE: Celebrities.java
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor(staticName = "invite")
public class Celebrities {
    private List<String> names;

    public List<String> getNames() {
        return names;
    }
}

// FILE: main.kt

import kotlin.test.assertTrue

fun box(): String {
    val celebrities = Celebrities.invite(listOf())
    assertTrue(celebrities.names.isEmpty())

    return "OK"
}
