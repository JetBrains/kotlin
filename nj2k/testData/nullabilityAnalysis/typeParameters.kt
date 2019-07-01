fun foo() {
    val l: List<Int> = List<Int>(1, {1})
    val a: Array<Int> = arrayOfNulls<Int>(42)
    val b: Array<Int> = arrayOf<Int>(1,2,3)
    val c: Array<Array<Int>> = arrayOf<Array<Int>>(arrayOfNulls<Int>(42))
    val d: Array<Array<Int>> = arrayOf<Array<Int>>(arrayOf<Int>(42))
    val e: Array<Array<Int>> = arrayOf<Array<Int>>(arrayOf<Int>(42, null))
    val f: Array<Array<Int>> = arrayOf<Array<Int>>(arrayOf<Int>(42, null), null)
    val g: Array<Array<Int>> = arrayOf<Array<Int>>(arrayOf<Int>(42), null)
    val h: Array<IntArray> = arrayOfNulls(5)
    val i: Array<Array<IntArray>> = Array<Array<IntArray>>(5, { arrayOfNulls<IntArray>(5) })
    val k: Array<Int> = arrayOf<Int>(null)
}