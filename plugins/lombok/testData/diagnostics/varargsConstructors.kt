// FILE: VarargsConstructor.java

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class VarargsConstructor {
    public String[] field1;

    public VarargsConstructor(String... names) { // Error in Lombok: 'VarargsConstructor(String...)' is already defined in 'VarargsConstructor'
        field1 = new String[] {"vararg"};
    }
}

// FILE: test.kt

fun test() {
    VarargsConstructor("vararg1", "vararg2") // Make sure the error about constructors ambiguty doesn't crash the compiler
}
