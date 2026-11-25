// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

// FILE: VarargsAndArrayField.java
import lombok.AllArgsConstructor;

// The generated constructor would be `VarargsAndArrayField(String[])`, the same JVM signature as the explicit vararg one,
// so it is not generated. NB: real Lombok generates it anyway, and `javac` reports "constructor is already defined".
@AllArgsConstructor
public class VarargsAndArrayField {
    public String[] names;

    public VarargsAndArrayField(String... names) {
        this.names = names;
    }
}

// FILE: test.kt
fun main() {
    VarargsAndArrayField("a", "b")
    VarargsAndArrayField(*arrayOf("a", "b"))
    // Only a non-vararg `(Array<String>)` constructor could accept an array without a spread
    VarargsAndArrayField(<!ARGUMENT_TYPE_MISMATCH!>arrayOf("a", "b")<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, stringLiteral */
