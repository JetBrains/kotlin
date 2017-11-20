package test

import kotlinx.android.parcel.Parcelize
import android.os.Parcelable

@Parcelize
interface <error descr="[PLUGIN_ERROR] 'Parcelable' should be a class">Intf</error> : Parcelable

@Parcelize
object <error descr="[PLUGIN_ERROR] 'Parcelable' should be a class">Obj</error>

class A {
    @Parcelize
    companion <error descr="[PLUGIN_ERROR] 'Parcelable' should be a class">object</error> {
        fun foo() {}
    }
}

@Parcelize
enum class <error descr="[PLUGIN_ERROR] 'Parcelable' should be a class">Enum</error> {
    WHITE, BLACK
}

@Parcelize
annotation class <error descr="[PLUGIN_ERROR] 'Parcelable' should be a class">Anno</error>
