// WITH_RUNTIME
// FIX: Change call to 'map'

fun foo(c: Collection<String>) {
    c.<caret>mapNotNull label@{
        return@label ""
    }
}