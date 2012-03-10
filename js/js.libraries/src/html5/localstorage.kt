package html5.localstorage

import js.native

native
val localStorage : LocalStorageClass = js.noImpl
native
class LocalStorageClass() {
    fun getItem(key : String) : Any? = js.noImpl
    fun setItem(key : String, value : Any?) : Unit = js.noImpl
}