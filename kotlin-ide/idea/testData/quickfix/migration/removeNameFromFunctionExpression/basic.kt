// "Remove identifier from anonymous function" "true"

fun foo() {
    (fun bar<caret>() {
        return@bar
    })
}
