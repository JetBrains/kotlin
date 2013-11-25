package foo

data class ThreeThings(val id1: String, val number: Int) {
    val id2: String = "id2"
        get() = $id2
    fun component3() = this.id2
}

fun returnThreeThings(): ThreeThings {
    return ThreeThings("id1", 42)
}

fun box(): String {
    val (id1, num, id2) = returnThreeThings()
    if ((id1 == "id1") and (num == 42) and (id2 == "id2"))
        return "OK"
    return "Unexpected result: id1 = ${id1}, num = ${num}, id2 = ${id2}"
}
