// FIR_IDENTICAL
// ISSUE: KT-73102

// FILE: JavaBaseClass.java

@lombok.experimental.SuperBuilder
public class JavaBaseClass {
    private String x;
    public abstract static class JavaBaseClassBuilder {  } // error: type JavaBaseClassBuilder does not take parameters
}

// FILE: test.kt

fun test() {
    val javaBaseClass = JavaBaseClass.builder().x("base").build()
}
