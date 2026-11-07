// MODULE: main
// FILE: sample/Main.kt
package sample

class Outer {
    class Nested
    typealias Alias = String
    var value: String = ""
    fun value() {}
    companion object {
        fun member() {}
    }
}

typealias Alias = Outer
enum class Choice { ENTRY }
fun top() {}
val top: Int = 0

// FILE: sample/JavaClass.java
package sample;

public class JavaClass {
    public int field;
    public static int staticField;
    public JavaClass() {}
    public void method() {}
    public static void staticMethod() {}
    public String getText() { return ""; }
}

// FILE: Root.kt
class RootClass
