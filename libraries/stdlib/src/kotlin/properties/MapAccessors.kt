package kotlin.properties

import java.io.Serializable
import kotlin.platform.platformName

// extensions for Map and MutableMap

public fun <V> Map<in String, *>.get(thisRef: Any?, property: PropertyMetadata): V = getOrImplicitDefault(property.name) as V

platformName("getVar")
public fun <V> MutableMap<in String, in V>.get(thisRef: Any?, property: PropertyMetadata): V = getOrImplicitDefault(property.name) as V

public fun <V> MutableMap<in String, in V>.set(thisRef: Any?, property: PropertyMetadata, value: V) {
    this.put(property.name, value)
}





// Map accessors

public interface MapAccessor<KMap, in KAccess, out V>
{
    public val map: Map<in KMap, V>

    /** Returns `true` if the map contains the specified [key]. */
    public fun containsKey(key: KAccess): Boolean

    /** Returns the value corresponding to the given [key], or the default value if such a key is not present in the map. */
    public fun getOrDefault(key: KAccess): V

    public open fun withDefault<V>(default: (KMap) -> V): MapAccessor<KMap, KAccess, V>
}

public interface MutableMapAccessor<KMap, in KAccess, V>: MapAccessor<KMap, KAccess, V> {
    public override val map: MutableMap<in KMap, V>
    /** Associates the specified [value] with the specified [key] in the map.  */
    public fun put(key: KAccess, value: V): V?

    public override fun <V> withDefault(default: (KMap) -> V): MutableMapAccessor<KMap, KAccess, V>
}


public fun <V> Map<in String, V>.forProperties(): MapAccessor<String, PropertyMetadata, V> = MapAccessorImpl(this, propertyNameSelector)

// todo: inline keyselector into anonymous object
public fun <K, V> Map<in K, V>.forProperties(keySelector: (PropertyMetadata) -> K): MapAccessor<K, PropertyMetadata, V> = MapAccessorImpl(this, keySelector)

public fun <V> MutableMap<in String, V>.forProperties(): MutableMapAccessor<String, PropertyMetadata, V> = MutableMapAccessorImpl(this, propertyNameSelector)

// todo: inline keyselector into anonymous object
public fun <K, V> MutableMap<in K, V>.forProperties(keySelector: (PropertyMetadata) -> K): MutableMapAccessor<K, PropertyMetadata, V> = MutableMapAccessorImpl(this, keySelector)


// extensions to delegate properties to MapAccessors

public fun <K, V> MapAccessor<K, PropertyMetadata, V>.get(thisRef: Any?, property: PropertyMetadata): V = this.getOrDefault(property)

platformName("getVar")
public fun <K, V> MutableMapAccessor<K, PropertyMetadata, in V>.get(thisRef: Any?, property: PropertyMetadata): V = this.getOrDefault(property) as V

public fun <K, V> MutableMapAccessor<K, PropertyMetadata, in V>.set(thisRef: Any?, property: PropertyMetadata, value: V) {
    this.put(property, value)
}



// MapAccessor implementations

private open class MapAccessorImpl<KMap, in KAccess, out V>(public override val map: Map<in KMap, V>, public val transform: (KAccess) -> KMap, public val default: (KMap) -> V = { key -> throwKeyNotFound(map, key) })
: MapAccessor<KMap, KAccess, V> {

    override fun containsKey(key: KAccess): Boolean = map.containsKey(transform(key))

    override fun getOrDefault(key: KAccess): V = transform(key).let { map.getOrElse(it, { default(it) }) }

    override fun withDefault<V>(default: (KMap) -> V): MapAccessor<KMap, KAccess, V> = MapAccessorImpl<KMap, KAccess, V>(map as Map<in KMap, V>, transform, default)
}


private class MutableMapAccessorImpl<KMap, in KAccess, V>(public override val map: MutableMap<in KMap, V>, transform: (KAccess) -> KMap, default: (KMap) -> V = { key -> throwKeyNotFound(map, key) })
: MapAccessorImpl<KMap, KAccess, V>(map, transform, default), MutableMapAccessor<KMap, KAccess, V> {


    override fun put(key: KAccess, value: V): V? = map.put(transform(key), value)

    override fun <V> withDefault(default: (KMap) -> V): MutableMapAccessor<KMap, KAccess, V> = MutableMapAccessorImpl<KMap, KAccess, V>(map as MutableMap<in KMap, V>, transform, default)
}
