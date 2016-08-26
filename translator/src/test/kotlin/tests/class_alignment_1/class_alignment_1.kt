class class_alignment_1_class(var field1: Long, var field2: Int, var field3: Long)


fun class_alignment_1(): Long {
    val b = class_alignment_1_class(100L, 200, 300L)
    assert(b.field1 == 100L)
    assert(b.field2 == 200)
    assert(b.field3 == 300L)
    return b.field3
}