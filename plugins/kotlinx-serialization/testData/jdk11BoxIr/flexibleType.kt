// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: J.java

import java.util.*;

public class J {
    public static String s() {
        return "OK";
    }

    public static List<String> ls() {
        return new ArrayList<>(List.of("OK"));
    }
}

// FILE: main.kt

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * K1 generally treat all flexible types as nullable, so we need to do the same in K2.
 * For MutableList vs List, we actually do not care, since they have the same serializer.
 */
@Serializable
class A {
    var s = J.s()
    var ls = J.ls()
}

fun box(): String {
    val a = A()
    a.s = null
    a.ls.add(null)
    val s = Json.encodeToString(a)
    if (s != """{"s":null,"ls":["OK",null]}""") return s
    return Json.decodeFromString<A>(s).s ?: "OK"
}
