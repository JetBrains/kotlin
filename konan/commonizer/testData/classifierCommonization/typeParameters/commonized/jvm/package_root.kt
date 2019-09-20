class A1 {
    val property: Any get() = TODO()
    fun function(value: Any): Any = value

    class Nested {
        val property: Any get() = TODO()
        fun function(value: Any): Any = value
    }

    inner class Inner {
        val property: Any get() = TODO()
        fun function(value: Any): Any = value
    }
}
actual class A2<T : Any?> actual constructor() {
    actual val property: T get() = TODO()
    actual fun function(value: T): T = value

    actual class Nested<T : Any?> actual constructor() {
        actual val property: T get() = TODO()
        actual fun function(value: T): T = value
    }

    actual inner class Inner actual constructor() {
        actual val property: T get() = TODO()
        actual fun function(value: T): T = value
    }
}
class A3<R : Any?> {
    val property: R get() = TODO()
    fun function(value: R): R = value

    class Nested<R : Any?> {
        val property: R get() = TODO()
        fun function(value: R): R = value
    }

    inner class Inner {
        val property: R get() = TODO()
        fun function(value: R): R = value
    }
}
class A4<T : Any> {
    val property: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T : Any> {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }
}
class A5<T : CharSequence> {
    val property: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T : CharSequence> {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }
}
class A6<T : String> {
    val property: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T : String> {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }
}
class A7<String> {
    val property: String get() = TODO()
    fun function(value: String): String = value

    class Nested<String> {
        val property: String get() = TODO()
        fun function(value: String): String = value
    }

    inner class Inner {
        val property: String get() = TODO()
        fun function(value: String): String = value
    }
}

class B1 {
    val property: Any get() = TODO()
    fun function(value: Any): Any = value

    class Nested {
        val property: Any get() = TODO()
        fun function(value: Any): Any = value
    }

    inner class Inner {
        val property: Any get() = TODO()
        fun function(value: Any): Any = value
    }
}
class B2<T : Any?> {
    val property: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T : Any?> {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }
}
class B3<R : Any?> {
    val property: R get() = TODO()
    fun function(value: R): R = value

    class Nested<R : Any?> {
        val property: R get() = TODO()
        fun function(value: R): R = value
    }

    inner class Inner {
        val property: R get() = TODO()
        fun function(value: R): R = value
    }
}
actual class B4<T : Any> actual constructor() {
    actual val property: T get() = TODO()
    actual fun function(value: T): T = value

    actual class Nested<T : Any> actual constructor() {
        actual val property: T get() = TODO()
        actual fun function(value: T): T = value
    }

    actual inner class Inner actual constructor() {
        actual val property: T get() = TODO()
        actual fun function(value: T): T = value
    }
}
class B5<T : CharSequence> {
    val property: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T : CharSequence> {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }
}
class B6<T : String> {
    val property: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T : String> {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }
}
class B7<String> {
    val property: String get() = TODO()
    fun function(value: String): String = value

    class Nested<String> {
        val property: String get() = TODO()
        fun function(value: String): String = value
    }

    inner class Inner {
        val property: String get() = TODO()
        fun function(value: String): String = value
    }
}

class C1 {
    val property: Any get() = TODO()
    fun function(value: Any): Any = value

    class Nested {
        val property: Any get() = TODO()
        fun function(value: Any): Any = value
    }

    inner class Inner {
        val property: Any get() = TODO()
        fun function(value: Any): Any = value
    }
}
class C2<T : Any?> {
    val property: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T : Any?>{
        val property: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }
}
class C3<R : Any?> {
    val property: R get() = TODO()
    fun function(value: R): R = value

    class Nested<R : Any?> {
        val property: R get() = TODO()
        fun function(value: R): R = value
    }

    inner class Inner {
        val property: R get() = TODO()
        fun function(value: R): R = value
    }
}
class C4<T : Any> {
    val property: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T : Any> {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }
}
actual class C5<T : CharSequence> actual constructor() {
    actual val property: T get() = TODO()
    actual fun function(value: T): T = value

    actual class Nested<T : CharSequence> actual constructor() {
        actual val property: T get() = TODO()
        actual fun function(value: T): T = value
    }

    actual inner class Inner actual constructor() {
        actual val property: T get() = TODO()
        actual fun function(value: T): T = value
    }
}
class C6<T : String> {
    val property: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T : String> {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }
}
class C7<String> {
    val property: String get() = TODO()
    fun function(value: String): String = value

    class Nested<String> {
        val property: String get() = TODO()
        fun function(value: String): String = value
    }

    inner class Inner {
        val property: String get() = TODO()
        fun function(value: String): String = value
    }
}

class D1 {
    val property: Any get() = TODO()
    fun function(value: Any): Any = value

    class Nested {
        val property: Any get() = TODO()
        fun function(value: Any): Any = value
    }

    inner class Inner {
        val property: Any get() = TODO()
        fun function(value: Any): Any = value
    }
}
class D2<T : Any?> {
    val property: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T : Any?> {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }
}
class D3<R : Any?> {
    val property: R get() = TODO()
    fun function(value: R): R = value

    class Nested<R : Any?> {
        val property: R get() = TODO()
        fun function(value: R): R = value
    }

    inner class Inner {
        val property: R get() = TODO()
        fun function(value: R): R = value
    }
}
class D4<T : Any> {
    val property: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T : Any> {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }
}
class D5<T : CharSequence> {
    val property: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T : CharSequence> {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }
}
actual class D6<T : String> actual constructor() {
    actual val property: T get() = TODO()
    actual fun function(value: T): T = value

    actual class Nested<T : String> actual constructor() {
        actual val property: T get() = TODO()
        actual fun function(value: T): T = value
    }

    actual inner class Inner actual constructor() {
        actual val property: T get() = TODO()
        actual fun function(value: T): T = value
    }
}
class D7<String> {
    val property: String get() = TODO()
    fun function(value: String): String = value

    class Nested<String> {
        val property: String get() = TODO()
        fun function(value: String): String = value
    }

    inner class Inner {
        val property: String get() = TODO()
        fun function(value: String): String = value
    }
}

class E1 {
    val property: Any get() = TODO()
    fun function(value: Any): Any = value

    class Nested {
        val property: Any get() = TODO()
        fun function(value: Any): Any = value
    }

    inner class Inner {
        val property: Any get() = TODO()
        fun function(value: Any): Any = value
    }
}
class E2<T : Any?> {
    val property: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T : Any?> {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }
}
class E3<R : Any?> {
    val property: R get() = TODO()
    fun function(value: R): R = value

    class Nested<R : Any?> {
        val property: R get() = TODO()
        fun function(value: R): R = value
    }

    inner class Inner {
        val property: R get() = TODO()
        fun function(value: R): R = value
    }
}
class E4<T : Any> {
    val property: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T : Any> {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }
}
class E5<T : CharSequence> {
    val property: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T : CharSequence> {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }
}
class E6<T : String> {
    val property: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T : String> {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: T): T = value
    }
}
actual class E7<String> actual constructor() {
    actual val property: String get() = TODO()
    actual fun function(value: String): String = value

    actual class Nested<String> actual constructor() {
        actual val property: String get() = TODO()
        actual fun function(value: String): String = value
    }

    actual inner class Inner actual constructor() {
        actual val property: String get() = TODO()
        actual fun function(value: String): String = value
    }
}

actual class F1<T> actual constructor() {
    actual val property: T get() = TODO()
    actual fun function(value: T): T = value

    actual class Nested<T> actual constructor() {
        actual val property: T get() = TODO()
        actual fun function(value: T): T = value
    }

    actual inner class Inner actual constructor() {
        actual val property: T get() = TODO()
        actual fun function(value: T): T = value
    }
}
class F2<in T> {
    val property: String get() = TODO()
    fun function(value: T): Any = TODO()

    class Nested<in T> {
        val property: String get() = TODO()
        fun function(value: T): Any = TODO()
    }

    inner class Inner {
        val property: String get() = TODO()
        fun function(value: T): Any = TODO()
    }
}
class F3<out T> {
    val property: T get() = TODO()
    fun function(value: Any): T = TODO()

    class Nested<out T> {
        val property: T get() = TODO()
        fun function(value: Any): T = TODO()
    }

    inner class Inner {
        val property: T get() = TODO()
        fun function(value: Any): T = TODO()
    }
}

actual class G1<T, R> actual constructor() {
    actual val property1: T get() = TODO()
    actual val property2: R get() = TODO()
    actual fun function(value: T): R = TODO()

    actual class Nested<T, R> actual constructor() {
        actual val property1: T get() = TODO()
        actual val property2: R get() = TODO()
        actual fun function(value: T): R = TODO()
    }

    actual inner class Inner actual constructor() {
        actual val property1: T get() = TODO()
        actual val property2: R get() = TODO()
        actual fun function(value: T): R = TODO()
    }
}
class G2<T> {
    val property1: T get() = TODO()
    val property2: T get() = TODO()
    fun function(value: T): T = value

    class Nested<T> {
        val property1: T get() = TODO()
        val property2: T get() = TODO()
        fun function(value: T): T = value
    }

    inner class Inner {
        val property1: T get() = TODO()
        val property2: T get() = TODO()
        fun function(value: T): T = value
    }
}
class G3<R> {
    val property1: R get() = TODO()
    val property2: R get() = TODO()
    fun function(value: R): R = value

    class Nested<R> {
        val property1: R get() = TODO()
        val property2: R get() = TODO()
        fun function(value: R): R = value
    }

    inner class Inner {
        val property1: R get() = TODO()
        val property2: R get() = TODO()
        fun function(value: R): R = value
    }
}
class G4<R, T> {
    val property1: T get() = TODO()
    val property2: R get() = TODO()
    fun function(value: T): R = TODO()

    class Nested<R, T> {
        val property1: T get() = TODO()
        val property2: R get() = TODO()
        fun function(value: T): R = TODO()
    }

    inner class Inner {
        val property1: T get() = TODO()
        val property2: R get() = TODO()
        fun function(value: T): R = TODO()
    }
}

actual class H1<T> actual constructor() {
    actual val dependentProperty: T get() = TODO()
    actual fun dependentFunction(value: T): T = value

    actual val T.dependentExtensionProperty: T get() = this
    actual fun T.dependentExtensionFunction(): T = this

    actual fun <T> independentFunction(): T = TODO()

    actual val <T> T.independentExtensionProperty: T get() = this
    actual fun <T> T.independentExtensionFunction(): T = this
}
actual class H2<T> actual constructor() {
    actual val dependentProperty: T get() = TODO()
    actual fun dependentFunction(value: T): T = value

    actual val T.dependentExtensionProperty: T get() = this
    actual fun T.dependentExtensionFunction(): T = this

    fun independentFunction(): T = TODO()

    val T.independentExtensionProperty: T get() = this
    fun T.independentExtensionFunction(): T = this
}

actual class I<T : I<T>> actual constructor() {
    actual val property: T get() = TODO()
    actual fun function(value: T): T = value
}
