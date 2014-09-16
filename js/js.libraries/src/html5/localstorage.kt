package html5.localstorage

native
public val localStorage : LocalStorageClass = noImpl

native
public class LocalStorageClass() {
    public fun getItem(key : String) : Any? = noImpl
    public fun setItem(key : String, value : Any?) : Unit = noImpl
}
