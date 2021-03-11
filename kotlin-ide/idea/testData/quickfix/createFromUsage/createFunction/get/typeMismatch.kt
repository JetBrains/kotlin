// "Create extension function 'A.get'" "true"
class A

fun A.get(i: Int) = this

fun test() {
    A()[<caret>"1"]
}