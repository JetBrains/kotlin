//FILE: ConstructorExample.java

import lombok.*;

@AllArgsConstructor
@RequiredArgsConstructor
public class ConstructorExample<A, B> {

    @Getter @Setter private int age = 10;

    @Getter private final A name;

    private B otherField;

    static void javaUsage() {
        val generated = new ConstructorExample<Long, Boolean>(12, 42L, true);
        val generatedReq = new ConstructorExample<String, Boolean>("234");
    }
}


//FILE: test.kt

object Test {
    fun usage() {
        val generated = ConstructorExample<Long, Boolean>(12, 42L, true)
        val generatedReq = ConstructorExample<String, Boolean>("234");
    }
}
