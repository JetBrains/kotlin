import org.jetbrains.annotations.PropertyKey

private val BUNDLE_NAME = "FooBundle"

public fun message(@PropertyKey(resourceBundle = "FooBundle") key: String) = key
public fun message2(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String) = key

fun test() {
    message("foo.bar")
}