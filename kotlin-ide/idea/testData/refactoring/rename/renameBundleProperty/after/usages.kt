import org.jetbrains.annotations.PropertyKey

public fun message(@PropertyKey(resourceBundle = "TestBundle") key: String) = key

fun test() {
    @PropertyKey(resourceBundle = "TestBundle") val s1 = "bar.foo"
    message("bar.foo")
}