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
    builder.<!FUNCTION_CALL_EXPECTED, NO_VALUE_FOR_PARAMETER!>str<!> // Access to field despite the fact it's invisible
    builder.<!FUNCTION_CALL_EXPECTED, NO_VALUE_FOR_PARAMETER!>id<!> // Access to field despite the fact it's invisible
}
