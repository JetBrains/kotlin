package k

public class Class() {
    public val prop: Int = 0
    fun function() = 1

    fun <T : Number, G> function2(
            b: Byte, c: Char, s: Short, i: Int, l: Long, bool: Boolean, f: Float, d: Double,
            ba: ByteArray, ca: CharArray, ia: IntArray, la: LongArray, boola: BooleanArray, fa: FloatArray, da: DoubleArray, sa: Array<String>,
            baa: Array<ByteArray>, saa: Array<Array<String>>,
            t: T, g: G, str: String, nestedClass: Class.F, innerClass: Class.G, nestedNested: Class.F.F
    ) {
    }

    class F {
        fun function() = 1

        class F {
            fun function() = 1
        }
    }

    inner class G {
        fun function() = 5
    }
}

public enum class EnumClass {
    ENTRY
}


public fun topLevelFun() {
}

public class ClassWithClassObject {
    companion object {
        fun f() = 1
    }
}

public object KotlinObject {
    fun f() = 1
}

public interface StaticFieldInClassObjectInTrait {
    companion object {
        public const val XX: String = "xx"
    }
}

object PlatformStaticFun {
    @JvmStatic
    fun test() {
    }
}

interface TraitNoImpl {
    fun foo()
}

public class TraitWithDelegatedNoImpl(f: TraitNoImpl): TraitNoImpl by f

interface TraitWithImpl {
    fun foo() = 1
}

public class TraitWithDelegatedWithImpl(f: TraitWithImpl) : TraitWithImpl by f

@kotlin.jvm.JvmOverloads
public fun withJvmOverloads(i: Int, b: Boolean = false, s: String="hello") {}

annotation class KAnno(val c: Int = 4, val d: String)
