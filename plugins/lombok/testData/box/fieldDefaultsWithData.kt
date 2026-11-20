// ISSUE: KT-89742, KT-89188
// FILE: DataExample.java

import lombok.*;
import lombok.experimental.*;
import org.jetbrains.annotations.*;

@Data
@FieldDefaults(level=AccessLevel.PRIVATE)
public class DataExample {
    // Fields should have no initializers, and be either final or @NotNull
    @NotNull String first;

    final int second;

    final boolean third;

    final String[] fourth;
}

// FILE: test.kt

import kotlin.test.*

fun box(): String {
    // @RequiresArgsConstructor
    val example = DataExample("Alpha", 2, true, arrayOf("ping", "pong"))
    // @Getter (fields should be private)
    assertEquals("Alpha", example.first)
    assertEquals(2, example.second)
    // By the way, example.third does not work here because of our accessor generation logic
    assertTrue(example.isThird)
    assertEquals("ping", example.fourth[0])
    assertEquals("pong", example.fourth[1])

    // @Setter
    example.first = "Omega"

    val other = DataExample("Omega", 2, true, arrayOf("ping", "pong"))
    // @ToString
    assertEquals("DataExample(first=Omega, second=2, third=true, fourth=[ping, pong])", other.toString())
    // @EqualsAndHashCode
    assertEquals(other, example)
    assertEquals(other.hashCode(), example.hashCode())

    return "OK"
}
