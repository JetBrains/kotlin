// FILE: AllArgsAndExplicit.java

import lombok.AllArgsConstructor;

// The explicit constructor has as many parameters as the generated one, but of another type,
// so it does not prevent `AllArgsAndExplicit(String)` from being generated
@AllArgsConstructor
public class AllArgsAndExplicit {
    public String field;

    public AllArgsAndExplicit(int value) {
        field = "explicit";
    }

    static void test() {
        if (!"explicit".equals(new AllArgsAndExplicit(1).field)) throw new AssertionError();
        if (!"generated".equals(new AllArgsAndExplicit("generated").field)) throw new AssertionError();
    }
}

// FILE: RequiredArgsAndExplicit.java

import lombok.RequiredArgsConstructor;

// The same for `@RequiredArgsConstructor`: `RequiredArgsAndExplicit(String)` is generated next to the explicit one
@RequiredArgsConstructor
public class RequiredArgsAndExplicit {
    public final String field;
    public int notRequired;

    public RequiredArgsAndExplicit(int value) {
        field = "explicit";
    }

    static void test() {
        if (!"explicit".equals(new RequiredArgsAndExplicit(1).field)) throw new AssertionError();
        if (!"generated".equals(new RequiredArgsAndExplicit("generated").field)) throw new AssertionError();
    }
}

// FILE: AllArgsAndExplicitWithSwappedParameters.java

import lombok.AllArgsConstructor;

// The parameter types are the same, but in another order, so `(String, int)` is generated next to the explicit `(int, String)`
@AllArgsConstructor
public class AllArgsAndExplicitWithSwappedParameters {
    public String field;
    public int number;

    public AllArgsAndExplicitWithSwappedParameters(int number, String field) {
        this.field = "explicit";
        this.number = number;
    }

    static void test() {
        if (!"explicit".equals(new AllArgsAndExplicitWithSwappedParameters(1, "generated").field)) throw new AssertionError();
        if (!"generated".equals(new AllArgsAndExplicitWithSwappedParameters("generated", 1).field)) throw new AssertionError();
    }
}

// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    AllArgsAndExplicit.test()
    assertEquals("explicit", AllArgsAndExplicit(1).field)
    assertEquals("generated", AllArgsAndExplicit("generated").field)

    RequiredArgsAndExplicit.test()
    assertEquals("explicit", RequiredArgsAndExplicit(1).field)
    assertEquals("generated", RequiredArgsAndExplicit("generated").field)

    AllArgsAndExplicitWithSwappedParameters.test()
    assertEquals("explicit", AllArgsAndExplicitWithSwappedParameters(1, "generated").field)
    assertEquals("generated", AllArgsAndExplicitWithSwappedParameters("generated", 1).field)

    return "OK"
}
