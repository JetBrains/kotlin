@Repeatable
@java.lang.annotation.Inherited
annotation class Anno(val name: String)

@Anno("Mary")
open class Base

@Anno("Tom")
class Impl : Base()