// "Create extension function 'Int.invoke'" "true"
// WITH_RUNTIME

class A<T>(val n: T)

fun test(): A<String> {
    return <caret>1(2, "2")
}