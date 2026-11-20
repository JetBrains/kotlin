// ISSUE: KT-89742, KT-90271
// DUMP_IR
// FILE: MakeFinalExample.java

import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@Getter
@FieldDefaults(makeFinal=true)
public class MakeFinalExample {
    int defaultField = 1;
    @NonFinal int nonFinalField = 2;
    public int publicField = 3;
}

// FILE: main.kt

import kotlin.test.assertEquals

fun box(): String {
    val example = MakeFinalExample()

    // defaultField becomes final, accessible directly (package-private)
    assertEquals(1, example.defaultField)

    // nonFinalField: @NonFinal overrides makeFinal
    example.nonFinalField = 20
    assertEquals(20, example.nonFinalField)

    // publicField becomes final too
    assertEquals(3, example.publicField)

    return "OK"
}
