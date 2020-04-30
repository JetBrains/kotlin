// MODE: inheritors

<# block [ 1 Implementation] #>
interface SomeInterface {
<# block [     1 Override] #>
    open val interfaceProperty: String
}

class SomeClass : SomeInterface {
    override val interfaceProperty: String = "overridden" // <== (1)
}