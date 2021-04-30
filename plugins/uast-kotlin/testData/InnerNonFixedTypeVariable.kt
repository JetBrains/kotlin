// !IGNORE_FIR

class Some<T : Some<T>>
fun test(list: List<Any>) {
    list.filterIsInstance<Some>().mapTo(mutableSetOf()) {

    }
}
