iimport

fun stdout(): java.io.PrintStream? {
  println("stdout")
  return System.out
}

fun main(args : Array<String>) {
  stdout()?.println("Hello, world!")
}