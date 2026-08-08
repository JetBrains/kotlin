// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: test.kt

fun box(): String {
    val c = Class.forName("_1__2")
    val ctor = c.getDeclaredConstructor(Array<String>::class.java)
    return c.getDeclaredMethod("getResult").invoke(ctor.newInstance(emptyArray<String>())) as String
}

// FILE: 1#@2.kts

val result = "OK"
