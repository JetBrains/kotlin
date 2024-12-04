// FILE: ConstructorExample.java

import lombok.*;

@AllArgsConstructor
public class ConstructorExample {

    // Part of constructor because not final.
    @Getter @Setter private int age = 10;

    // Part of constructor because not initialized.
    @Getter(AccessLevel.PROTECTED) private String name;

    // Part of constructor because not initialized.
    private final boolean otherField;

    // Not part of constructor because final and initialized.
    @Getter private final String result = "OK";

    // Not part of constructor because static.
    private static String constant;

    static void javaUsage() {
        val generated = new ConstructorExample(12, "sdf", true);
    }
}


// FILE: test.kt

fun box(): String {
    val generated = ConstructorExample(12, "sdf", true)
    return generated.getResult()
}
