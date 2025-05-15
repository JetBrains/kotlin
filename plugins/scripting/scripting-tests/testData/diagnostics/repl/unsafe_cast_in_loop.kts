// SNIPPET

var num: Int? = 3
val numNotNull = num!!
for (x in 1..2) {
    val num2 = num
    println(num + 1)
    num = null
}
