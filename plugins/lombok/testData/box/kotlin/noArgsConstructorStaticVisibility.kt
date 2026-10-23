// ISSUE: KT-89212
// FULL_JDK
// FIR_DUMP
// DUMP_KT_IR
// FILE: JavaClass.java

@lombok.NoArgsConstructor(staticName = "of")
public class JavaClass {
    int field;
}

// FILE: test.kt

import lombok.NoArgsConstructor
import java.lang.reflect.Modifier

@NoArgsConstructor(staticName = "of")
class KotlinClass(var a: Int)

fun box(): String {
    // Lombok hides the no-args constructor of a Java class behind the static factory.
    if (!Modifier.isPrivate(JavaClass::class.java.getDeclaredConstructor().modifiers)) return "Fail: Java no-args constructor is not private"
    JavaClass.of()

    KotlinClass.of()
    if (!Modifier.isPrivate(KotlinClass::class.java.getDeclaredConstructor().modifiers)) return "Fail: Kotlin no-args constructor is not private"

    return "OK"
}
