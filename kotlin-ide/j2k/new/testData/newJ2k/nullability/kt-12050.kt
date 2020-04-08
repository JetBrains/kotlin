class NullableArray {
    fun createArrayFailure(size: Int): Array<String?> {
        return arrayOfNulls(size)
    }

    fun createArraySuccess(size: Int): Array<String> {
        return arrayOf()
    }
}