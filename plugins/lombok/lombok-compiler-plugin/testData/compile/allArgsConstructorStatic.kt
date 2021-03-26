//FILE: ConstructorExample.java

import lombok.*;

@AllArgsConstructor(staticName = "of")
public class ConstructorExample {

    @Getter @Setter private int age = 10;

    @Getter(AccessLevel.PROTECTED) private String name;

    private boolean otherField;

    public ConstructorExample(String arg) {

    }

    static void javaUsage() {
        ConstructorExample existing = new ConstructorExample("existing");
        ConstructorExample generated = ConstructorExample.of(45, "234", false);
    }
}


//FILE: test.kt

object Test {
    fun usage() {
        val existing: ConstructorExample = ConstructorExample("existing")
        val generated: ConstructorExample = ConstructorExample.of(45, "234", false)
    }
}
