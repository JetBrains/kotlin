annotation class NoArg

@NoArg
interface BaseIntf

open class Test1(a: String) : BaseIntf

class Test12(b: Int) : Test1("")

@NoArg
abstract class BaseClass

class MyClass(a: String) : BaseClass()