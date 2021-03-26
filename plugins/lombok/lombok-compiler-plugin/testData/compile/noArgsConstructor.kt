//FILE: ConstructorExample.java

import lombok.*;

@NoArgsConstructor
public class ConstructorExample {

    public ConstructorExample(String arg) {

    }

    @Getter @Setter private int age = 10;

    @Getter(AccessLevel.PROTECTED) private String name;

    static void javaUsage() {
        val existing = new ConstructorExample("existing");
        val generated = new ConstructorExample();
    }
}


//FILE: test.kt

object Test {
    fun usage() {
        val existing = ConstructorExample("existing")
        val generated = ConstructorExample()
    }
}
