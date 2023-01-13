private var myProperty: Int? = null

interface Interface {
    var someVar: Int?
        get() = myProperty
        set(value) {
            myProperty = value
        }
}
