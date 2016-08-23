fun nullable_primitive_types_1_small(): Int {
    var variable: Int? = null
    variable = 29
    val art: Int? = variable
    return art!!
}

fun nullable_primitive_types_1_extern(x:  Int): Int {
    val variable: Int? = x
    val art: Int? = variable!! + 8
    return art!!
}

fun nullable_primitive_types_1_if_null(): Int {
    val variable: Int? = null
    if (variable == null){
        return 11
    }
    else{
        return 2245
    }
}

fun nullable_primitive_types_1_if_not_null(): Int {
    val variable: Int? = null
    if (variable != null){
        return 11
    }
    else{
        return 2245
    }
}