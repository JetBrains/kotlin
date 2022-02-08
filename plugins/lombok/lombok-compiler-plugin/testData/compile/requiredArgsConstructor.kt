//FILE: ConstructorExample.java

import lombok.*;

@RequiredArgsConstructor
public class ConstructorExample {

    //not required
    @Getter @Setter private int age;

    //required final
    @Getter @Setter private final String foo;

    //not required final, because has initializer
    @Getter @Setter private final int bar = 234;

    //not required
    @Getter(AccessLevel.PROTECTED) private String name;

    //required by annotation
    @NonNull
    private boolean otherField;

    //not required by annotation, because has initializer
    @NonNull
    private Long zzzz = 23L;

    static void javaUsage() {
        ConstructorExample generated = new ConstructorExample("foo", true);
    }
}


//FILE: test.kt

class Test {
    fun run() {
        val generated = ConstructorExample("foo", true)
        assertEquals(generated.foo, "foo")
    }
}
