// MODULE: main
// FILE: sample/Declarations.kt
package sample

interface First<T> {
    fun transform(value: T): T
    val text: T
}

interface Second {
    fun transform(value: String): String
    val text: String
}

open class Base : First<String>, Second {
    override fun transform(value: String): String = value
    override var text: String = ""
        protected set
    override fun toString(): String = text
    fun plain() {}
}

open class Child : Base() {
    override fun transform(value: String): String = value
    override var text: String = ""
        protected set
}

class Inherited : Child()

abstract class Text : CharSequence {
    override val length: Int get() = 1
}

class KotlinChild : JavaBase<String>() {
    override fun transform(value: String): String = value
}

// FILE: sample/JavaBase.java
package sample;

public class JavaBase<T> {
    public T transform(T value) { return value; }
}

// FILE: sample/JavaChild.java
package sample;

public class JavaChild extends JavaBase<String> {
    @Override
    public String transform(String value) { return value; }
}

// FILE: sample/JavaGrandChild.java
package sample;

public class JavaGrandChild extends JavaChild {
    @Override
    public String transform(String value) { return value; }
}
