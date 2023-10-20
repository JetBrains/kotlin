// USE_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>mid_increased_by_one<!>(arr: List<Int>, target: Int): Boolean {
    val size = arr.size
    val mid = arr.size / 2 + 1 // if arr.size == 1, mid is out of bounds
    return when {
        arr.isEmpty() -> false
        <!VIPER_VERIFICATION_ERROR!>arr[mid]<!> == target -> true
        arr[mid] < target -> mid_increased_by_one(arr.subList(mid + 1, size), target)
        else -> mid_increased_by_one(arr.subList(0, mid), target)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>mid_decreased_by_one<!>(arr: List<Int>, target: Int): Boolean {
    val size = arr.size
    val mid = arr.size / 2 - 1 // if arr.size == 1, mid is out of bounds
    return when {
        arr.isEmpty() -> false
        <!VIPER_VERIFICATION_ERROR!>arr[mid]<!> == target -> true
        arr[mid] < target -> mid_decreased_by_one(arr.subList(mid + 1, size), target)
        else -> mid_decreased_by_one(arr.subList(0, mid), target)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>mid_decreased_by_one_in_rec_call<!>(arr: List<Int>, target: Int): Boolean {
    val size = arr.size
    val mid = arr.size / 2
    return when {
        arr.isEmpty() -> false
        arr[mid] == target -> true
        arr[mid] < target -> mid_decreased_by_one_in_rec_call(arr.subList(mid + 1, size), target)
        // if arr.size == 1, arr.subList(0, -1) is called
        else -> mid_decreased_by_one_in_rec_call(<!VIPER_VERIFICATION_ERROR!>arr.subList(0, mid - 1)<!>, target)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>unsafe_binary_search<!>(arr: List<Int>, target: Int, left: Int, right: Int): Boolean {
    if (left > right) {
        return false
    }

    val mid = left + (right - left) / 2

    return when {
        <!VIPER_VERIFICATION_ERROR!>arr[mid]<!> == target -> true
        arr[mid] < target -> unsafe_binary_search(arr, target, mid + 1, right)
        else -> unsafe_binary_search(arr, target, left, mid - 1)
    }
}
