class NullableArray {
    fun createArrayFailure(size: Int): Array<String?> {
        return arrayOfNulls<String?>(size)
    }

    fun createArraySuccess(size: Int): Array<String> {
        return arrayOf()
    }
}