// FIR_IDENTICAL

// FILE: JavaConstant.java

public class JavaConstant {
    private JavaConstant() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    public static String CONST = "CONST";
}

// FILE: LombokExample.java

import lombok.Builder;

@Builder
public class LombokExample {
    private String name;
    private int age;
}

// FILE: test.kt

import JavaConstant.CONST
import LombokExample

data class Context(
   val id: String,
)

fun test() {
    val context = Context("test")
    println(CONST)
    
    // Use Lombok builder to ensure the builder generator is triggered
    val example = LombokExample.builder()
        .name("Test")
        .age(25)
        .build()
}