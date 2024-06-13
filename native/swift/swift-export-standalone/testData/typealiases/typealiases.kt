// KIND: STANDALONE
// MODULE: main
// FILE: main.kt
package typealiases

import typealiases.inner.*

public typealias SmallInteger = Short

public typealias Bar = typealiases.inner.Bar

public class Foo

// FILE: inner.kt
package typealiases.inner

public typealias LargeInteger = Long

public typealias Foo = typealiases.Foo

public class Bar

// FILE: root.kt
public typealias RegularInteger = Int

public typealias DefaultInteger = RegularInteger

public fun increment(integer: DefaultInteger): RegularInteger = integer + 1

@Target(AnnotationTarget.TYPE)
annotation class UselessAnnotation

typealias ShouldHaveNoAnnotation = @UselessAnnotation Int

typealias UselessDeclaration = UselessAnnotation

// FILE: should_be_ignored.kt
import typealiases.Foo

public annotation class OptIn
typealias annotationClass = OptIn

enum class ENUM {
    A, B, C;
    class INSIDE_ENUM
}
typealias enumClass = ENUM

interface OUTSIDE_PROTO {
    open class INSIDE_PROTO
}
typealias outerInterface = OUTSIDE_PROTO
typealias innerInterface = OUTSIDE_PROTO.INSIDE_PROTO

class INHERITANCE_COUPLE : OUTSIDE_PROTO.INSIDE_PROTO(), OUTSIDE_PROTO
typealias inheritanceCouple = INHERITANCE_COUPLE

class INHERITANCE_SINGLE_PROTO : OUTSIDE_PROTO.INSIDE_PROTO()
typealias inhertanceSingleProto = INHERITANCE_SINGLE_PROTO

open class OPEN_CLASS
typealias openClass = OPEN_CLASS

class INHERITANCE_SINGLE_CLASS : OPEN_CLASS()
typealias inheritanceSingleClass = INHERITANCE_SINGLE_CLASS

data class DATA_CLASS(val a: Int)
typealias dataClass = DATA_CLASS

data class DATA_CLASS_WITH_REF(val o: Any)
typealias dataClassWithRef = DATA_CLASS_WITH_REF

inline class INLINE_CLASS(val a: Int)
typealias inlineClass = INLINE_CLASS

inline class INLINE_CLASS_WITH_REF(val i: DATA_CLASS_WITH_REF)
typealias inlineClassWithRef = INLINE_CLASS_WITH_REF

abstract class ABSTRACT_CLASS
typealias abstractClss = ABSTRACT_CLASS

sealed class SEALED {
    object O : SEALED()
}
typealias sealedClass = SEALED

object OBJECT_WITH_CLASS_INHERITANCE: OPEN_CLASS()
typealias objectWithClassInheritance = OBJECT_WITH_CLASS_INHERITANCE

object OBJECT_WITH_INTERFACE_INHERITANCE: OUTSIDE_PROTO
typealias objectWithInterfaceInheritance = OBJECT_WITH_INTERFACE_INHERITANCE

// copied from std, the simpliest generic inheritance that I could come up with.
object OBJECT_WITH_GENERIC_INHERITANCE: ListIterator<Nothing> {
    override fun hasNext(): Boolean = false
    override fun hasPrevious(): Boolean = false
    override fun nextIndex(): Int = 0
    override fun previousIndex(): Int = -1
    override fun next(): Nothing = throw NoSuchElementException()
    override fun previous(): Nothing = throw NoSuchElementException()
}
typealias objectWithGenericInheritance = OBJECT_WITH_GENERIC_INHERITANCE

data object DATA_OBJECT_WITH_PACKAGE {
    fun foo(): Int = 5
    val value: Int = 5
    var variable: Int = 5
}
typealias dataObjectWithPackage = DATA_OBJECT_WITH_PACKAGE

typealias closure = () -> Unit

typealias nullable_primitive = Int?
typealias nullable_class = Foo?

typealias never = Nothing
