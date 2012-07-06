package foo

fun box() : Int {
    var sum = 0
    for (i in 0.upto(5)) {
      sum += i
    }
    return sum
}