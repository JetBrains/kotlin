import javaApi.Base

internal class C : Base() {
    fun f() {
        val other = Base()
        val value = other.property + property
        other.property = 1
        property = other.property + value
        getBase(property).property = 0
    }

    private fun getBase(i: Int): Base {
        return Base()
    }
}
