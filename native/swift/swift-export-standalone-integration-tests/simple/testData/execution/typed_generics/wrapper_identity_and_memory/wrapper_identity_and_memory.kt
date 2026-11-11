// KIND: STANDALONE
// MODULE: WrapperIdentity
// SWIFT_EXPORT_CONFIG: enableGenericsSupport=true
// FILE: WrapperIdentity.kt
@file:OptIn(kotlin.ExperimentalStdlibApi::class, kotlin.native.runtime.NativeRuntimeApi::class)

import kotlin.native.runtime.GC

class Plain(val id: Int)

class FinalBox<T>(var value: T)
open class OpenBox<T>(var value: T)
class ChildBox(value: Plain) : OpenBox<Plain>(value)
class OutBox<out T>(val value: T)

interface Reader<T> {
    fun read(): T
}

private class HiddenReader(private val plain: Plain) : Reader<Plain> {
    override fun read(): Plain = plain
}

sealed class Resource<out T> {
    class Success<T>(val data: T) : Resource<T>()
    object Loading : Resource<Nothing>()
}

private val sharedFinal = FinalBox(Plain(1))
private val sharedOpen = OpenBox(Plain(2))
private val sharedChild = ChildBox(Plain(3))
private val sharedReader: Reader<Plain> = HiddenReader(Plain(4))
private val sharedSuccess: Resource<Plain> = Resource.Success(Plain(5))

// One Kotlin object, returned again and again.
fun finalBox(): FinalBox<Plain> = sharedFinal
fun finalBoxAsStar(): FinalBox<*> = sharedFinal
fun openBox(): OpenBox<Plain> = sharedOpen
fun childAsOpenBox(): OpenBox<Plain> = sharedChild
fun hiddenReader(): Reader<Plain> = sharedReader
fun success(): Resource<Plain> = sharedSuccess

// A new Kotlin object per call.
fun newFinalBox(): FinalBox<Plain> = FinalBox(Plain(6))
fun newOpenBox(): OpenBox<Plain> = OpenBox(Plain(7))
fun newHiddenReader(): Reader<Plain> = HiddenReader(Plain(8))
fun boxes(count: Int): List<FinalBox<Int>> = List(count) { FinalBox(it) }

fun isSharedFinal(box: FinalBox<Plain>): Boolean = box === sharedFinal
fun <T> identity(value: T): T = value

fun collect() {
    GC.collect()
    GC.collect()
}
