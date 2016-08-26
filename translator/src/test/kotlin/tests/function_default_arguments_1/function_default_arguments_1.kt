class function_default_arguments_1_argClass(val classArgument: Int)

fun function_default_arguments_1_add(x: Int, t: function_default_arguments_1_argClass = function_default_arguments_1_argClass(124), y: Int = 53, z: Int = 777): Int {
    return x + y
}

fun function_default_arguments_1_add_class(x: Int, t: function_default_arguments_1_argClass = function_default_arguments_1_argClass(124), y: Int = 53, z: Int = 777): Int {
    return x + t.classArgument
}

fun function_default_arguments_1_add_class_arg(): Int {
    return function_default_arguments_1_add_class(190)
}

fun function_default_arguments_1_mixed(): Int {
    return function_default_arguments_1_add(23, t = function_default_arguments_1_argClass(29), z = 8)
}

fun function_default_arguments_1_last_missed_fun(z: Int, u: Int = 954): Int {
    return z + u
}

fun function_default_arguments_1_last_missed(x: Int): Int {
    return function_default_arguments_1_last_missed_fun(x)
}

fun function_default_arguments_1_last_present(x: Int): Int {
    return function_default_arguments_1_last_missed_fun(x, 29)
}

fun function_default_arguments_1_position_fun(first: Int, second: Int = 324
                                              , third: Int = 2531): Int {
    return first + 2 * second + 3 * third
}

fun function_default_arguments_1_position_third_only(x: Int): Int {
    return function_default_arguments_1_position_fun(11, third = x)
}

fun function_default_arguments_1_position_all_reverse(x: Int): Int {
    return function_default_arguments_1_position_fun(11, third = x, second = 32)
}

fun function_default_arguments_1_position_all_right_order(x: Int): Int {
    return function_default_arguments_1_position_fun(11, second = 32, third = x)
}

fun function_default_arguments_1_position_all_default(x: Int): Int {
    return function_default_arguments_1_position_fun(x)
}

fun function_default_arguments_1_all_position_fun(first: Int = 31, second: Int = 32
                                                  , third: Int = 33): Int {
    return first + 2 * second + 3 * third
}

fun function_default_arguments_1_all_position(): Int {
    return function_default_arguments_1_all_position_fun()
}