// EXPECTED_REACHABLE_NODES: 505
package foo

class Runner {
    public fun run(f: () -> Unit): Unit = f()
}

class Counter() {
    var count = 0
    val runner = Runner()

    public fun count(n: Int) {
        for (i in 1..n) {
            tick()
        }
    }

    public fun getCount(): Int = count

    private inline fun tick()  {
        runner.run { count++ }
    }
}

fun add(a: Int, b: Int): Int {
    val counter = Counter()
    counter.count(a)
    counter.count(b)
    return counter.getCount()
}

fun box(): String {
    assertEquals(3, add(1, 2))
    assertEquals(7, add(3, 4))

    return "OK"
}