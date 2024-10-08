// FIR_IDENTICAL

// FILE: MethodPreserving.java

import lombok.Builder;

// Lombok should keep all manually written methods and skip generated ones (based both on specification and real behavior)
@Builder
public class MethodPreserving {
    private String str;
    private int integer;
    private char character;

    public static class MethodPreservingBuilder {
        public void name(int x, int y) {
        }

        public int integer(int i) {
            return i;
        }
    }
}

// FILE: test.kt

fun test() {
    val builder: MethodPreserving.MethodPreservingBuilder = MethodPreserving.builder();
    builder.<!UNRESOLVED_REFERENCE!>name<!>(3, 4); // No error: call the manually written method
    builder.<!UNRESOLVED_REFERENCE!>name<!>("str3"); // Error: method is no generated because a method with the same name already exists

    val intResult: Int = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>builder.integer(200)<!>; // No error: call the manually written method

    val builder2: MethodPreserving.MethodPreservingBuilder = builder.character('c'); // No error: call a generated method
}
