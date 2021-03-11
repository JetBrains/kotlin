// PSI_ELEMENT: com.intellij.lang.properties.psi.Property
// FIND_BY_REF
import org.jetbrains.annotations.PropertyKey

public fun message(@PropertyKey(resourceBundle = "propertyUsagesByRef.2") key: String) = key

fun test() {
    message("<caret>foo.bar")
}