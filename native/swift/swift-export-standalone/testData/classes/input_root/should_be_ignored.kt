public annotation class OptIn

enum class ENUM {
    A, B, C;
    class INSIDE_ENUM
}

interface OUTSIDE_PROTO {
    open class INSIDE_PROTO
}

class INHERITANCE_COUPLE : OUTSIDE_PROTO.INSIDE_PROTO(), OUTSIDE_PROTO
class INHERITANCE_SINGLE_PROTO : OUTSIDE_PROTO.INSIDE_PROTO()

open class OPEN_CLASS

class INHERITANCE_SINGLE_CLASS : OPEN_CLASS()

data class DATA_CLASS(val a: Int)

data class DATA_CLASS_WITH_REF(val o: Any)

inline class INLINE_CLASS(val a: Int)

inline class INLINE_CLASS_WITH_REF(val i: DATA_CLASS_WITH_REF)

abstract class ABSTRACT_CLASS

sealed class SEALED {
    object O : SEALED()
}

object OBJECT_WITH_CLASS_INHERITANCE: OPEN_CLASS()

object OBJECT_WITH_INTERFACE_INHERITANCE: OUTSIDE_PROTO

// copied from std, the simpliest generic inheritance that I could come up with.
object OBJECT_WITH_GENERIC_INHERITANCE: ListIterator<Nothing> {
    override fun hasNext(): Boolean = false
    override fun hasPrevious(): Boolean = false
    override fun nextIndex(): Int = 0
    override fun previousIndex(): Int = -1
    override fun next(): Nothing = throw NoSuchElementException()
    override fun previous(): Nothing = throw NoSuchElementException()
}

data object DATA_OBJECT_WITH_PACKAGE {
    fun foo(): Int = 5
    val value: Int = 5
    var variable: Int = 5
}