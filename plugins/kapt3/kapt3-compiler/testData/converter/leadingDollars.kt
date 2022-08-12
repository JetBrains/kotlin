// FIR_BLOCKED: LC don't support names with $
// CORRECT_ERROR_TYPES

// FILE: $Test.java
public class $Test {
    public static class $Inner {}
}

// FILE: Test$.java
public class Test$ {
    public static class Inner$ {}
}

// FILE: test.kt

package test

fun test(a: `$Test`.`$Inner`) {}

fun test(a: `Test$`.`Inner$`) {}
