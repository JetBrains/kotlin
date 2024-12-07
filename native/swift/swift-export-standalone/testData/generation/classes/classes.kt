// KIND: STANDALONE
// MODULE: main
// FILE: class_with_deeper_package.kt
package namespace.deeper

class NAMESPACED_CLASS

class Foo {
    class INSIDE_CLASS {
        class DEEPER_INSIDE_CLASS {
            fun foo(): Boolean = TODO()

            val my_value: UInt = 5u

            var my_variable: Long = 5
        }

        fun foo(): Boolean = TODO()

        val my_value: UInt = 5u

        var my_variable: Long = 5
    }

    fun foo(): Boolean = TODO()

    val my_value: UInt = 5u

    var my_variable: Long = 5

}


// FILE: class_with_package.kt
package namespace

/**
 *  demo comment for
 *  NAMESPACED_CLASS
 */
class NAMESPACED_CLASS

class Foo {
    /**
     * this is a sample comment for INSIDE_CLASS with package
     */
    class INSIDE_CLASS

    /**
     * this is a sample comment for func on class with package
     */
    fun foo(): Boolean = TODO()

    /**
     * this is a sample comment for val on class with package
     */
    val my_value: UInt = 5u

    /**
     * this is a sample comment for var on class with package
     */
    var my_variable: Long = 5
}

// FILE: classes.kt
/**
 * this is a sample comment for class without public constructor
 */
public class ClassWithNonPublicConstructor internal constructor(public val a: Int)

/**
 * this is a sample comment for class without package
 * in order to support documentation for primary constructor - we will have to start parsing comment content:
 * https://kotlinlang.org/docs/kotlin-doc.html#constructor
 */
class Foo (a: Int) {
    /**
     * this is a sample comment for secondary constructor
     */
    constructor(f: Float) : this(f.toInt())

    /**
     * this is a sample comment for private constructor
     */
    private constructor(d: Double) : this(d.toInt())

    /**
     * this is a sample comment for INSIDE_CLASS without package
     */
    class INSIDE_CLASS {
        /**
         * this is a sample comment for func on INSIDE_CLASS without package
         */
        fun my_func(): Boolean = TODO()

        /**
         * this is a sample comment for val on INSIDE_CLASS without package
         */
        val my_value_inner: UInt = 5u

        /**
         * this is a sample comment for var on INSIDE_CLASS without package
         */
        var my_variable_inner: Long = 5
    }
    /**
     * this is a sample comment for func on class without package
     */
    fun foo(): Boolean = TODO()

    /**
     * this is a sample comment for val on class without package
     */
    val my_value: UInt = 5u

    /**
     * this is a sample comment for var on class without package
     */
    var my_variable: Long = 5

    /**
     * this is a sample comment for Foo's Companion object without package
     */
    companion object {
        /**
         * this is a sample comment for fun in Foo.Comanion without package
         */
        fun my_func(): Boolean = TODO()

        /**
         * this is a sample comment for val in Foo.Comanion without package
         */
        val my_value_inner: UInt = 5u

        /**
         * this is a sample comment for var in Foo.Comanion without package
         */
        var my_variable_inner: Long = 5
    }
}

class CLASS_WITH_SAME_NAME {
    fun foo(): Int = TODO()
}

abstract class ABSTRACT_CLASS

// FILE: object.kt
/**
demo comment for packageless object
 */
object OBJECT_NO_PACKAGE {
    class Foo
    class Bar(val i: Int) {
        fun bar(): Int = 5
        class CLASS_INSIDE_CLASS_INSIDE_OBJECT

        companion object NamedCompanion {
            fun foo(): Int = 5
        }
    }

    object OBJECT_INSIDE_OBJECT
    internal object INTERNAL_OBJECT_INSIDE_OBJECT

    fun foo(): Int = 5
    val value: Int = 5
    var variable: Int = 5
}

private object PRIVATE_OBJECT


// FILE: object_with_package.kt
package namespace.deeper

/**
demo comment for packaged object
 */
object OBJECT_WITH_PACKAGE {
    class Foo
    class Bar(val i: Int) {
        fun bar(): Int = 5

        /**
         * demo comment for inner object
         */
        object OBJECT_INSIDE_CLASS
    }

    object OBJECT_INSIDE_OBJECT
    internal object INTERNAL_OBJECT_INSIDE_OBJECT

    fun foo(): Int = 5
    val value: Int = 5
    var variable: Int = 5
}

private object PRIVATE_OBJECT

data object DATA_OBJECT_WITH_PACKAGE {
    fun foo(): Int = 5
    val value: Int = 5
    var variable: Int = 5
}

// FILE: same_name_class.kt
package why_we_need_module_names
import CLASS_WITH_SAME_NAME

class CLASS_WITH_SAME_NAME {
    fun foo(): Unit = TODO()
}

fun foo() = CLASS_WITH_SAME_NAME()

/**
 * this will calculate the return type of `foo` on `CLASS_WITH_SAME_NAME`.
 * Return type of CLASS_WITH_SAME_NAME differs, so we can detect which one was used on Swift side.
 * We are expecting it to be the one that does not have a module - so it will be Swift.Int32.
 */
fun bar() = foo().foo()

// FILE: should_be_ignored.kt
public annotation class OptIn

enum class ENUM {
    A, B, C;
    class INSIDE_ENUM
}

interface OUTSIDE_PROTO {
    // FIXME: KT-70541
    // We can not properly detect nested classes as unsopported
     /*
    open class INSIDE_PROTO
     */
}

// FIXME: See the commend above on OUTSIDE_PROTO.INSIDE_PROTO
/*
    class INHERITANCE_COUPLE : OUTSIDE_PROTO.INSIDE_PROTO(), OUTSIDE_PROTO
    class INHERITANCE_SINGLE_PROTO : OUTSIDE_PROTO.INSIDE_PROTO()
*/

object OBJECT_WITH_INTERFACE_INHERITANCE: OUTSIDE_PROTO

data class DATA_CLASS(val a: Int)

data class DATA_CLASS_WITH_REF(val o: Any)

data class DATA_CLASS_WITH_MANY_FIELDS(val a: Int, val b: String, val c: Any) {
    val d get() = 1.0
    val e get() = "e"
}

inline class INLINE_CLASS(val a: Int)

inline class INLINE_CLASS_WITH_REF(val i: DATA_CLASS_WITH_REF)

sealed class SEALED {
    class C : SEALED()
    object O : SEALED()
}

sealed interface SEALED_INTERFACE

// copied from std, the simpliest generic inheritance that I could come up with.
object OBJECT_WITH_GENERIC_INHERITANCE: ListIterator<Nothing> {
    override fun hasNext(): Boolean = false
    override fun hasPrevious(): Boolean = false
    override fun nextIndex(): Int = 0
    override fun previousIndex(): Int = -1
    override fun next(): Nothing = throw NoSuchElementException()
    override fun previous(): Nothing = throw NoSuchElementException()
}

open class GENERIC_CLASS<T>

open class INHERITANCE_GENERIC : GENERIC_CLASS<Int>()

class INHERITANCE_UNSUPPORTED_BASE : INHERITANCE_GENERIC()

// MODULE: inheritance
// EXPORT_TO_SWIFT
// FILE: inheritance.kt

open class OPEN_CLASS()

class INHERITANCE_SINGLE_CLASS(var value: Int = 42) : OPEN_CLASS()

object OBJECT_WITH_CLASS_INHERITANCE: OPEN_CLASS()

// MODULE: cross_module_inheritance(inheritance)
// EXPORT_TO_SWIFT
// FILE: inheritance_across_modules.kt

class CLASS_ACROSS_MODULES(var value: Int): OPEN_CLASS()
