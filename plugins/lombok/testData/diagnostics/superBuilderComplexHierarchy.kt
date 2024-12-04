// FIR_IDENTICAL

// FILE: A.java

import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class A {
    public int a;
}

// FILE: B.java

import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class B extends A {
    public int b;
}

// FILE: I.java

public interface I {
    int i = 42;
}

// FILE: C.java

import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class C extends B implements I {
    public int c;
}

// FILE: test.kt

fun test() {
    val aSuperBuilder: A.ABuilder<*, *> = A.builder()
    val bSuperBuilder: B.BBuilder<*, *> = B.builder()
    val cSuperBuilder: C.CBuilder<*, *> = C.builder()

    val b2: B.BBuilder<*, *> = bSuperBuilder.b(20)

    val a: C.CBuilder<*, *> = cSuperBuilder.a(1)
    val b: C.CBuilder<*, *> = a.b(2)
    val c: C.CBuilder<*, *> = b.c(3)
    val result: C = c.build()
}