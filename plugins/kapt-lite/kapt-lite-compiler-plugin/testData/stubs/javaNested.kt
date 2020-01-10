// FILE: lib/Lib.java
package lib;

public class Lib {
    public static class Nested {
        public static void foo() {}
    }
}

// FILE: test.kt
fun foo(): lib.Lib.Nested? = null