// ISSUE: KT-89742
// DUMP_IR
// FILE: FieldDefaultsExample.java

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.experimental.PackagePrivate;

@Getter
@Setter
@FieldDefaults(makeFinal=true, level=AccessLevel.PRIVATE)
public class FieldDefaultsExample {
    public int publicField = 1;
    @NonFinal int nonFinalField = 2;
    @PackagePrivate int packagePrivateField = 3;
    int defaultField = 4;
}

// FILE: main.kt

import kotlin.test.assertEquals

fun box(): String {
    val example = FieldDefaultsExample()

    // publicField is public (explicit modifier overrides @FieldDefaults level)
    assertEquals(1, example.publicField)

    // nonFinalField: @NonFinal overrides makeFinal, but level=PRIVATE still applies (no explicit modifier)
    // Access via getter/setter since it becomes private
    example.nonFinalField = 20
    assertEquals(20, example.nonFinalField)

    // packagePrivateField: @PackagePrivate overrides level, but makeFinal still applies
    // May access directly as we are in the same package
    assertEquals(3, example.packagePrivateField)

    // defaultField: both makeFinal and level=PRIVATE apply
    // Access via getter since it becomes private final
    assertEquals(4, example.defaultField)

    return "OK"
}
