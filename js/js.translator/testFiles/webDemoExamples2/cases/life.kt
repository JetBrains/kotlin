/**
 * This is a straightforward implementation of The Game of Life
 * See http://en.wikipedia.org/wiki/Conway's_Game_of_Life
 */

import java.util.*
import js.*;

/*
 * A field where cells live. Effectively immutable
 */
class Field(
  val width : Int,
  val height : Int,
  // This function tells the constructor which cells are alive
  // if init(i, j) is true, the cell (i, j) is alive
  init : (Int, Int) -> Boolean
) {
  private val live : Array<Array<Boolean>> = Array(height) {i -> Array(width) {j -> init(i, j)}}

  private fun liveCount(i : Int, j : Int)
    = if (i in 0..height-1 &&
          j in 0..width-1 &&
          live[i][j]) 1 else 0

  // How many neighbors of (i, j) are alive?
  fun liveNeighbors(i : Int, j : Int) =
    liveCount(i - 1, j - 1) +
    liveCount(i - 1, j) +
    liveCount(i - 1, j + 1) +
    liveCount(i, j - 1) +
    liveCount(i, j + 1) +
    liveCount(i + 1, j - 1) +
    liveCount(i + 1, j) +
    liveCount(i + 1, j + 1)

  // You can say field[i, j], and this function gets called
  fun get(i : Int, j : Int) = live[i][j]
}

/**
 * This function takes the present state of the field
 * and return a new field representing the next moment of time
 */
fun next(field : Field) : Field {
  return Field(field.width, field.height) {i, j ->
    val n = field.liveNeighbors(i, j)
    if (field[i, j])
      // (i, j) is alive
      n in 2..3 // It remains alive iff it has 2 or 3 neighbors
    else
      // (i, j) is dead
      n == 3 // A new cell is born if there are 3 neighbors alive
  }
}

/** A few colony examples here */
fun main(args : Array<String>) {
  // Simplistic demo
  printField("***", 3)
  // "Star burst"
  printField("""
    __*__
    _***_
    __*__
  """, 10)
  // Stable colony
  printField("""
    __*__
    _*_*_
    __*__
  """, 3)
  // Stable from the step 2
  printField("""
    __**__
    __**__
    __**__
  """, 3)
  // Oscillating colony
  printField("""
    __**__
    __**__
      __**__
      __**__
  """, 6)
  // A fancier oscillating colony
  printField("""
    ---------------
    ---***---***---
    ---------------
    -*----*-*----*-
    -*----*-*----*-
    -*----*-*----*-
    ---***---***---
    ---------------
    ---***---***---
    -*----*-*----*-
    -*----*-*----*-
    -*----*-*----*-
    ---------------
    ---***---***---
    ---------------
  """, 10)
}

// UTILITIES

fun printField(s : String, steps : Int) {
  var field = makeField(s)
  for (step in 1..steps) {
    println("Step: $step")
    for (i in 0..field.height-1) {
      for (j in 0..field.width-1) {
        print(if (field[i, j]) "*" else " ")
      }
      println("")
    }
    field = next(field)
  }
}


fun <T> Array<T>.toList() : List<T> = this.to(ArrayList<T>())

val String?.size : Int
get() = if (this != null) this.length else 0;

fun <T, C: MutableCollection<T>> Array<T>.to(result: C) : C {
    for (elem in this)
        result.add(elem)
    return result
}

fun makeField(s : String) : Field {
  val lines : Array<String> = s.split("\n")

  val w = max<String>(lines.toList(), comparator<String> {o1, o2 ->
          val l1 : Int = o1.size
          val l2 = o2.size
          l1 - l2
  })!!
  val data = Array(lines.size) {Array(w.size) {false}}

  // workaround
  for (i in data.indices) {
    data[i] = Array(w.size) {false}
    for (j in data[i].indices)
      data[i][j] = false
  }

  for (line in lines.indices) {
    for (x in lines[line].indices) {
      val c = lines[line][x]
      data[line][x] = c == '*'
    }
  }

  return Field(w.size, lines.size) {i, j -> data[i][j]}
}

// An excerpt from the Standard Library
val String?.indices : IntRange get() = IntRange(0, this!!.size)

fun <K, V> MutableMap<K, V>.set(k : K, v : V) { put(k, v) }

val <T> Array<T>.isEmpty : Boolean get() = size == 0
