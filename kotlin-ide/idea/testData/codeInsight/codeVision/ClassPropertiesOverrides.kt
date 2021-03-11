// MODE: inheritors

<# block [ 1 Inheritor] #>
abstract class SomeClass {
<# block [     1 Override] #>
    abstract val someAbstractProperty: Int
<# block [     2 Overrides] #>
    open val nonAbstractProperty: Int = 10
    open val notToBeOverriddenProperty: Int = 10
}

<# block [ 1 Inheritor] #>
open class DerivedClassA : SomeClass() {
    override val someAbstractProperty: Int = 5
<# block [     1 Override] #>
    override val nonAbstractProperty: Int = 15 // NOTE that DerivedClassB overrides both getter and setter but counted once
}

class DerivedClassB : DerivedClassA() {
    override var nonAbstractProperty: Int = 15
        get() = 20
        set(value) {field = value / 2}
}