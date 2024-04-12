package why_we_need_module_names
import CLASS_WITH_SAME_NAME

class CLASS_WITH_SAME_NAME {
    fun foo(): Unit = TODO()
}

fun foo() = CLASS_WITH_SAME_NAME()

/**
 * this will calculate the return type of `foo` on `CLASS_WITH_SAME_NAME`.
 * Return type of CLASS_WITH_SAME_NAME differs, so we can detect which one was used on Swift side.
 * We are expecting it to be the one that does not have a module - so it will be Swift.Int32.
 */
fun bar() = foo().foo()
