// IGNORE_BACKEND_K1: ANY
// FULL_JDK
// WITH_STDLIB

// FILE: X.java

import lombok.Data;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class X extends Y {
    public int b;

    @Singular("singleBList") private java.util.List bList;
}

// FILE: Y.java

import lombok.Data;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@SuperBuilder(setterPrefix = "set")
@Data
public class Y {
    public int a;

    @Singular private java.lang.Iterable<String> aStrings;
}

// FILE: I.java

public interface I {
    int i = 42;
}

// FILE: Z.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Z extends X implements I {
    public int c;
}

// FILE: test.kt

fun box(): String {
    val c = Z.builder()
        .setA(1)
        .setAString("a")
        .clearAStrings()
        .setAString("a1")
        .setAStrings(listOf("a2", "a3"))
        .b(2)
        .singleBList("b1")
        .c(3)
        .build()

    return if (c.a == 1 && c.aStrings == listOf("a1", "a2", "a3") &&
        c.b == 2 && c.bList == listOf("b1") &&
        c.c == 3
    ) {
        "OK"
    } else {
        "Error: $c"
    }
}