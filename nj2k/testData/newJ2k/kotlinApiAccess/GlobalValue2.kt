import kotlinApi.globalValue2

internal class C {
    fun foo(): Int {
        globalValue2 = 0
        return globalValue2
    }
}