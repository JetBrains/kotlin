// USE_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>safe_binary_search<!>(arr: List<Int>, target: Int): Boolean {
    val size = arr.size
    val mid = arr.size / 2 // +1 or -1 throws IndexOutOfBound
    return when {
        arr.isEmpty() -> false
        arr[mid] == target -> true
        arr[mid] < target -> safe_binary_search(arr.subList(mid + 1, size), target)
        else -> safe_binary_search(arr.subList(0, mid), target) // mid - 1 throws IllegalArgumentException
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>unsafe_binary_search_fixed<!>(arr: List<Int>, target: Int, left: Int, right: Int): Boolean {
    if (left > right || left < 0 || right >= arr.size) {
        return false
    }

    val mid = left + (right - left) / 2

    return when {
        arr[mid] == target -> true
        arr[mid] < target -> unsafe_binary_search_fixed(arr, target, mid + 1, right)
        else -> unsafe_binary_search_fixed(arr, target, left, mid - 1)
    }
}
