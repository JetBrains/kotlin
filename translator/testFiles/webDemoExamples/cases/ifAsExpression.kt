fun main(args : Array<String>) {
  System.out?.println(max(Integer.parseInt(args[0]), Integer.parseInt(args[1])))
}

fun max(a : Int, b : Int) = if (a > b) a else b