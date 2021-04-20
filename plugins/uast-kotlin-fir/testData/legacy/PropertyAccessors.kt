class PropertyTest {
    var stringRepresentation: String
        get() = this.toString()
        set(value) {
            setDataFromString(value)
        }

    fun setDataFromString(data: String) {

    }
}
