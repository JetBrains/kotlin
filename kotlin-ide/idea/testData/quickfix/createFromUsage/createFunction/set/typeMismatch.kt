// "Create extension function 'A.set'" "true"
class A

fun A.set(i: Int, j: Int) {

}

fun test() {
    A()[<caret>"1"] = 2
}