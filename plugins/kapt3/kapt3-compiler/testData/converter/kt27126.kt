// WITH_RUNTIME

@file:Suppress("NOTHING_TO_INLINE")
package test

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun charSequence(key: String? = null) = object : BundleProperty<CharSequence>(key) {
    override fun getValue(bundle: Any, key: String): CharSequence? = TODO()
    override fun setValue(bundle: Any, key: String, value: CharSequence) {}
}

abstract class NullableBundleProperty<EE>(private val key: String?) : ReadWriteProperty<Any, EE?> {
    private inline fun KProperty<*>.toKey(): String {
        return toString()
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): EE? = getValue(thisRef, key ?: property.toKey())
    override fun setValue(thisRef: Any, property: KProperty<*>, value: EE?) {
        setNullableValue(thisRef, key ?: property.toKey(), value)
    }

    abstract fun getValue(bundle: Any, key: String): EE?
    abstract fun setNullableValue(bundle: Any, key: String, value: EE?)
}

abstract class BundleProperty<AA>(key: String?) : NullableBundleProperty<AA>(key) {

    final override fun setValue(thisRef: Any, property: KProperty<*>, value: AA?) {
        super.setValue(thisRef, property, value)
    }

    final override fun getValue(thisRef: Any, property: KProperty<*>): AA = super.getValue(thisRef, property)!!
    final override fun setNullableValue(bundle: Any, key: String, value: AA?) {
        setValue(bundle, key, value!!)
    }

    abstract fun setValue(bundle: Any, key: String, value: AA)
}