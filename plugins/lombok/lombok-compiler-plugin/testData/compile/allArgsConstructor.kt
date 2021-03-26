//FILE: ConstructorExample.java

import lombok.*;

@AllArgsConstructor
public class ConstructorExample {

    @Getter @Setter private int age = 10;

    @Getter(AccessLevel.PROTECTED) private String name;

    private boolean otherField;

    static void javaUsage() {
        val generated = new ConstructorExample(12, "sdf", true);
    }
}


//FILE: test.kt

object Test {
    fun usage() {
        val generated = ConstructorExample(12, "sdf", true)
    }
}
