// ISSUE: KT-86620

// FILE: VarargsConstructor.java

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class VarargsConstructor {
    public String field1;
    public String field2;

    public VarargsConstructor(String... names) {
        field2 = "vararg";
    }

    static void test() {
        if (!"vararg".equals(new VarargsConstructor("str0").field2)) throw new AssertionError();
        if (!"allArgsConstructor".equals(new VarargsConstructor("str0", "allArgsConstructor").field2)) throw new AssertionError();
    }
}

// FILE: VarargsConstructor2.java

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class VarargsConstructor2 {
    public String field1;

    public VarargsConstructor2(String... names) {
        field1 = "vararg";
    }

    static void test() {
        if (!"vararg".equals(new VarargsConstructor2("str0", "vararg").field1)) throw new AssertionError();
        if (!"allArgsConstructor".equals(new VarargsConstructor2("allArgsConstructor").field1)) throw new AssertionError();
    }
}

// FILE: test.kt

fun box(): String {
    VarargsConstructor.test()

    // The only signature with varags matches the current call-site
    assertEquals("vararg", VarargsConstructor("str0").field2)
    // The more more specific constructor overload with regular parameters should be chosen instead of the signature with varags
    assertEquals("allArgsConstructor", VarargsConstructor("str0", "allArgsConstructor").field2)

    VarargsConstructor2.test()

    // The only signature with varags matches the current call-site
    assertEquals("vararg", VarargsConstructor2("str0", "vararg").field1)
    // The more more specific constructor overload with regular parameters should be chosen instead of the signature with varags
    assertEquals("allArgsConstructor", VarargsConstructor2("allArgsConstructor").field1)

    return "OK"
}
