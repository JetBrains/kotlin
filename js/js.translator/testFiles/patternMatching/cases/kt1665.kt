fun main(args : Array<String>) {
  val a = 10
  val b = 3
  when {
    a > b -> println("a")
    b > a  -> println("b")
    else -> println("Unknown")
  }
}