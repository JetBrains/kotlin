class class_reassigment_1_class(val field: Int)

fun class_reassigment_1_base() : class_reassigment_1_class{
    val instance = class_reassigment_1_class(567)
    return instance
}

fun class_reassigment_1(): Int{
    var instance = class_reassigment_1_class(123)
    instance = class_reassigment_1_base()
    return instance.field
}