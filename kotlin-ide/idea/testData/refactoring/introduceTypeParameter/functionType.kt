// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

class A

fun foo(x: List<<selection>(A?) -> List<Int></selection>>) {

}

fun test() {
    foo(listOf({ a -> listOf(1) }))
}