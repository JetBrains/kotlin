// CORRECT_ERROR_TYPES
// FILE: I.java
public interface I {
    void f();
}

// FILE: J.java
public class J extends Unresolved implements I {}

// FILE: k.kt
class K : J()
