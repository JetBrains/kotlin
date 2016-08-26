class class_reassignment_1_class(val field: Int)

fun class_reassignment_1_base(): class_reassignment_1_class {
    val instance = class_reassignment_1_class(567)
    return instance
}

fun class_reassignment_1(): Int {
    var instance = class_reassignment_1_class(123)
    instance = class_reassignment_1_base()
    return instance.field
}