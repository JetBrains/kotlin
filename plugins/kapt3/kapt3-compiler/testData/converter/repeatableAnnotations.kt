// FILE: lib/Anno.java
package lib;
public @interface Anno {
    String[] construct() default {};
    String value();
}

//FILE: lib/R.java
package lib;

public class R {
    public static class id {
        public final static int textView = 100;
    }
}

// FILE: test.kt
// WITH_STDLIB
import lib.Anno
import kotlin.reflect.KClass

class Test {
    @Anno("1")
    @Anno(value = "2", construct = ["A", "B"])
    @Anno("3", construct = ["C"])
    val value: String = ""
}

annotation class AnnoChar(val x: Int, val chr: Char)
annotation class AnnoBoolean(val x: Int, val bool: Boolean)
annotation class AnnoInt(val x: Int, val i: Int)
annotation class AnnoLong(val x: Int, val l: Long)
annotation class AnnoFloat(val x: Int, val flt: Float)
annotation class AnnoDouble(val x: Int, val dbl: Double)

annotation class AnnoString(val x: Int, val s: String)

annotation class AnnoIntArray(val x: Int, val b: IntArray)
annotation class AnnoLongArray(val x: Int, val b: LongArray)

annotation class AnnoArray(val x: Int, val a: Array<String>)

annotation class AnnoClass(val x: Int, val c: KClass<Color>)

enum class Color { BLACK }
annotation class AnnoEnum(val x: Int, val c: Color)

@AnnoChar(lib.R.id.textView, 'c')
@AnnoBoolean(lib.R.id.textView, false)
@AnnoInt(lib.R.id.textView, 5)
@AnnoFloat(lib.R.id.textView, 1.0f)
@AnnoDouble(lib.R.id.textView, 4.0)
@AnnoString(lib.R.id.textView, "AAA")
@AnnoIntArray(lib.R.id.textView, [1, 2, 3])
@AnnoLongArray(lib.R.id.textView, [1L, 3L])
@AnnoArray(lib.R.id.textView, [ "A", "B" ])
@AnnoClass(lib.R.id.textView, Color::class)
class Test2