// MODE: inheritors

<# block [ 2 Inheritors] #>
abstract class SomeClass {
<# block [     1 Override] #>
    open fun someFun() = ""
<# block [     2 Implementations] #>
    abstract fun someAbstractFun()
}

class DerivedClassA : SomeClass {
    override fun someFun() = "overridden"
    override fun someAbstractFun() = "overridden"
}
class DerivedClassB : SomeClass {
    override fun someAbstractFun() = "overridden"
}
