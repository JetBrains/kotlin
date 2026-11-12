// RUN_PIPELINE_TILL: FRONTEND
// FILE: BuilderExample.java

import lombok.Builder;

@Builder
public class BuilderExample {
    public String str;
    public Int id;
}

// FILE: test.kt

fun test() {
    val builder = BuilderExample.builder()
    builder.<!INVISIBLE_REFERENCE!>str<!> // Access to field despite the fact it's invisible
    builder.<!INVISIBLE_REFERENCE!>id<!> // Access to field despite the fact it's invisible
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaProperty, localProperty, propertyDeclaration */
