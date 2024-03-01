public annotation class OptIn

enum ENUM {
    class INSIDE_ENUM
}

interface OUTSIDE_PROTO {
    class INSIDE_PROTO
}

class INHERITANCE_COUPLE : OUTSIDE_PROTO.INSIDE_PROTO, OUTSIDE_PROTO
class INHERITANCE_SINGLE_PROTO : OUTSIDE_PROTO.INSIDE_PROTO

open class OPEN_CLASS

class INHERITANCE_SINGLE_CLASS : OPEN_CLASS

object OBJECT

data class DATA_CLASS(val a: Int)

data class DATA_CLASS_WITH_REF(val o: OBJECT)

inline class INLINE_CLASS(val a: Int)

inline class INLINE_CLASS_WITH_REF(val i: DATA_CLASS_WITH_REF)

abstract class ABSTRACT_CLASS

sealed class SEALED {
    object O : SEALED()
}