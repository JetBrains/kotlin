// FILE: ConstructorExample.java

import lombok.*;

@AllArgsConstructor(staticName = "of")
@RequiredArgsConstructor(staticName = "of")
public class ConstructorExample<A, B> {

    @Getter @Setter private int age = 10;

    @Getter private final A name;

    private B otherField;

    static void javaUsage() {
        ConstructorExample<Long, Boolean> generated = ConstructorExample.of(12, 42L, true);
        ConstructorExample<String, Boolean> generatedReq = ConstructorExample.of("234");
    }
}


// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    val generated: ConstructorExample<Long, Boolean> = ConstructorExample.of(12, 42L, true)
    assertEquals(42L, generated.name)
    val generatedReq: ConstructorExample<String, Boolean> = ConstructorExample.of("234")
    assertEquals("234", generatedReq.name)
    return "OK"
}
