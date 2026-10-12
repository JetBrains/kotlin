// ISSUE: KT-89742
// DUMP_IR
// FILE: LevelOnlyExample.java

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.PackagePrivate;

@Getter
@Setter
@FieldDefaults(level=AccessLevel.PRIVATE)
public class LevelOnlyExample {
    int defaultField = 1;
    public int publicField = 2;
    @PackagePrivate int packagePrivateField = 3;
}

// FILE: main.kt

import kotlin.test.assertEquals

fun box(): String {
    val example = LevelOnlyExample()

    // defaultField becomes private, access via getter/setter
    example.defaultField = 10
    assertEquals(10, example.defaultField)

    // publicField keeps its explicit public modifier
    example.publicField = 20
    assertEquals(20, example.publicField)

    // packagePrivateField: @PackagePrivate overrides level
    assertEquals(3, example.packagePrivateField)

    return "OK"
}
