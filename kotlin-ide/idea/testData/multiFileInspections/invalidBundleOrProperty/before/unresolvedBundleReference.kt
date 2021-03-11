import org.jetbrains.annotations.PropertyKey

private val TEST_BUNDLE2 = "TestBundle2"

fun message(@PropertyKey(resourceBundle = "TestBundle2") key: String, vararg args: Any) = key
fun message2(@PropertyKey(resourceBundle = TEST_BUNDLE2) key: String, vararg args: Any) = key