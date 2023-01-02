private var myProperty: Int? = null

interface Interface {
    var someVar: Int?
        get() = myProperty?.let {
            if (it == 1) {
                it + 1
            } else {
                it
            }
        }
        set(value) {
            myProperty = value
        }
}
