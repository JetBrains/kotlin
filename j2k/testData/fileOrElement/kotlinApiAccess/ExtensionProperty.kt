import kotlinApi.*

internal class C {
    internal fun foo(): Int {
        "a".extensionProperty = 1
        return "b".extensionProperty
    }
}