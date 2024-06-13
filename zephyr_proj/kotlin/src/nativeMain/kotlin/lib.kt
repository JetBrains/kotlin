package example

object Object {
  val field = "A"
}

class MyClass(var brand: String, var cost: Int) {
  fun printCost() {
    println("Total cost for $brand is $cost");
  }
}

fun forIntegers(b: Byte, s: Short, i: UInt, l: Long) { }
fun forFloats(f: Float, d: Double) { }

fun strings(str: String) : String? {
  return "That is '$str' from C"
}

fun run() {
  val c = MyClass("Toyota", 80000);
  c.printCost();
}

val globalString = "A global String"
