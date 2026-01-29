package kotlin.collections

import java.util.Arrays

internal object ArraysUtilJVM {
    @JvmStatic
    fun <T> asList(array: Array<T>): MutableList<T> {
        return Arrays.asList(*array)
    }
}
