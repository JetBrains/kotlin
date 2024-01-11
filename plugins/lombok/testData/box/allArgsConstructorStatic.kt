// FILE: ConstructorExample.java

import lombok.*;

@AllArgsConstructor(staticName = "of")
public class ConstructorExample {

    // Part of constructor because not final.
    @Getter @Setter private int age = 10;

    // Part of constructor because not initialized.
    @Getter(AccessLevel.PROTECTED) private String name;

    // Part of constructor because not initialized.
    private boolean otherField;

    // Not part of constructor because final and initialized.
    @Getter private final String result = "OK";

    // Not part of constructor because static.
    private static String constant;

    public ConstructorExample(String arg) {

    }

    static void javaUsage() {
        ConstructorExample existing = new ConstructorExample("existing");
        ConstructorExample generated = ConstructorExample.of(45, "234", false);
    }
}


// FILE: test.kt

fun box(): String {
    val existing: ConstructorExample = ConstructorExample("existing")
    val generated: ConstructorExample = ConstructorExample.of(45, "234", false)
    return generated.getResult()
}
