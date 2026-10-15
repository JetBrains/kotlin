// ISSUE: KT-90271
// FILE: MakeFinalExample.java

import lombok.experimental.NonFinal;

public class MakeFinalExample {
    int defaultField = 1;
    @NonFinal int nonFinalField = 2;
    @NonFinal final int finalField = 3;
}

// FILE: main.kt

fun foo() {
    val example = MakeFinalExample()

    // defaultField becomes final, error should be here
    example.defaultField = 10

    // nonFinalField: @NonFinal overrides makeFinal
    example.nonFinalField = 20

    // finalField: @NonFinal cannot change explicit modifier, error should be here
    example.<!VAL_REASSIGNMENT!>finalField<!> = 30
}

// FILE: lombok.config
lombok.fieldDefaults.defaultFinal = true
