package stopInExtensionInlineCall

fun main(args: Array<String>) {
    val a = 1
    12.apply {
        {
            //Breakpoint!
            foo(a)
        }()
    }
}

inline fun <T> T.inlineApply(block: T.() -> kotlin.Unit) { this.block() }

fun foo(a: Any) {}
