// CURIOUS_ABOUT: test1, test2, test3
// WITH_STDLIB

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
class A(val value: Int) : Parcelable

@Parcelize
class B : Parcelable

@Parcelize
object C : Parcelable

fun test1() {
    parcelableCreator<A>()
    parcelableCreator<B>()
    parcelableCreator<C>()
}

inline fun <reified T : Parcelable> test2() {
    parcelableCreator<T>()
}

fun test3() {
    test2<A>()
}
