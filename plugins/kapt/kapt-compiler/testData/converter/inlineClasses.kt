@JvmInline
value class Id(val value: Int)

open class Base(id: Id)

class Derived : Base(Id(0))
