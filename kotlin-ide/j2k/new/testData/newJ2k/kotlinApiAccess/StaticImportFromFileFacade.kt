import kotlinApi.extensionFunction
import kotlinApi.extensionProperty

internal class C {
    fun foo(): Int {
        1.extensionFunction()
        "a".extensionProperty = 1
        return "b".extensionProperty
    }
}
