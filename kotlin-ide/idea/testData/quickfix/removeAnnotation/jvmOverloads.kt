// "Remove @JvmOverloads annotation" "true"
// WITH_RUNTIME

interface T {
    @kotlin.jvm.<caret>JvmOverloads fun foo(s: String = "OK")
}
