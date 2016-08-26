class nullable_class_fields_1_class {
    var left: nullable_class_fields_1_class? = null
    var right: Int? = null
}

fun nullable_class_fields_1_primitives(): Int {
    val a = nullable_class_fields_1_class()
    if (a.right == null) {
        return 29
    } else {
        return 28
    }
}

fun nullable_class_fields_1_classes(): Int {
    val a = nullable_class_fields_1_class()
    if (a.left == null) {
        return 29
    } else {
        return 28
    }
}

fun nullable_class_fields_1_classes_not_null(): Int {
    val a = nullable_class_fields_1_class()
    a.left = nullable_class_fields_1_class()
    if (a.left == null) {
        return 29
    } else {
        return 28
    }
}
