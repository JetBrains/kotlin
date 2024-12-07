package foo

inline fun block(p: () -> Int) = p()

fun createFunction(x: Int): () -> Int = { x }

// CHECK_BREAKS_COUNT: function=box count=0
// CHECK_LABELS_COUNT: function=box name=$l$block count=0
fun box(): String {
    assertEquals(23, block(createFunction(23)))
    return "OK"
}