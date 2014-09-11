package html5.localstorage

native
public val localStorage : LocalStorageClass = js.noImpl

native
public class LocalStorageClass() {
    public fun getItem(key : String) : Any? = js.noImpl
    public fun setItem(key : String, value : Any?) : Unit = js.noImpl
}
