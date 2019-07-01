import kotlinApi.globalValue1

internal class C {
    fun foo(): Int {
        globalValue1 = 0
        return globalValue1
    }
}