@java.lang.annotation.Inherited
annotation class Anno

@Anno
open class Base

class Impl : Base()

@Anno
interface Intf

class IntfImpl : Intf