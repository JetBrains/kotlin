expect class A2<T : Any?>() {
    val property: T
    fun function(value: T): T

    expect class Nested<T : Any?>() {
        val property: T
        fun function(value: T): T
    }

    expect inner class Inner() {
        val property: T
        fun function(value: T): T
    }
}

expect class B4<T : Any>() {
    val property: T
    fun function(value: T): T

    expect class Nested<T : Any>() {
        val property: T
        fun function(value: T): T
    }

    expect inner class Inner() {
        val property: T
        fun function(value: T): T
    }
}

expect class C5<T : CharSequence>() {
    val property: T
    fun function(value: T): T

    expect class Nested<T : CharSequence>() {
        val property: T
        fun function(value: T): T
    }

    expect inner class Inner() {
        val property: T
        fun function(value: T): T
    }
}

expect class D6<T : String>() {
    val property: T
    fun function(value: T): T

    expect class Nested<T : String>() {
        val property: T
        fun function(value: T): T
    }

    expect inner class Inner() {
        val property: T
        fun function(value: T): T
    }
}

expect class E7<String>() {
    val property: String
    fun function(value: String): String

    expect class Nested<String>() {
        val property: String
        fun function(value: String): String
    }

    expect inner class Inner() {
        val property: String
        fun function(value: String): String
    }
}

expect class F1<T>() {
    val property: T
    fun function(value: T): T

    expect class Nested<T>() {
        val property: T
        fun function(value: T): T
    }

    expect inner class Inner() {
        val property: T
        fun function(value: T): T
    }
}

expect class G1<T, R>() {
    val property1: T
    val property2: R
    fun function(value: T): R

    expect class Nested<T, R>() {
        val property1: T
        val property2: R
        fun function(value: T): R
    }

    expect inner class Inner() {
        val property1: T
        val property2: R
        fun function(value: T): R
    }
}

expect class H1<T>() {
    val dependentProperty: T
    fun dependentFunction(value: T): T

    val T.dependentExtensionProperty: T
    fun T.dependentExtensionFunction(): T

    fun <T> independentFunction(): T

    val <T> T.independentExtensionProperty: T
    fun <T> T.independentExtensionFunction(): T
}

expect class H2<T>() {
    val dependentProperty: T
    fun dependentFunction(value: T): T

    val T.dependentExtensionProperty: T
    fun T.dependentExtensionFunction(): T
}

expect class I<T : I<T>>() {
    val property: T
    fun function(value: T): T
}

expect interface J1<A> {
    fun a(): A
}

expect interface J2<A, B> {
    fun a(b: B): A
    fun b(a: A): B
}

expect interface J3<A, B, C> {
    fun a(b: B, c: C): A
    fun b(a: A, c: C): B
    fun c(a: A, b: B): C
}

expect class K<A, B : A, C : J1<B>, D : J2<C, A>>() : J3<D, C, B> {
    override fun a(b: C, c: B): D
    override fun b(a: D, c: B): C
    override fun c(a: D, b: C): B
    inner class Inner<C, D : C, E : J2<C, D>>() {
        fun dependentFunction(value: A): E
        fun <A : CharSequence, E : Number> independentFunction(value: A): E
    }
}
