@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*

public fun foo() {
    top_level_functions_foo()
}
public fun bar() {
    top_level_functions_bar()
}

@ImportedBridge("top_level_functions_foo")
internal external fun top_level_functions_foo(): Boolean

@ImportedBridge("top_level_functions_bar")
internal external fun top_level_functions_bar(): Boolean
