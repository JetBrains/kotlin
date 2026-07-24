// RUN_PIPELINE_TILL: BACKEND
// WITH_EXTRA_CHECKERS
// RETURN_VALUE_CHECKER_MODE: CHECKER

// FILE: Outer.java

public class Outer {
    public @interface Inner {}

    // The annotation type `Outer.Inner` is a nested classifier of the Java class currently being built.
    // Resolving it while computing the return-value status re-enters Outer's declaration list.
    @Inner
    public String annotated() {
        return "";
    }
}

// FILE: test.kt

fun usage(o: Outer) {
    Outer().annotated()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType */
