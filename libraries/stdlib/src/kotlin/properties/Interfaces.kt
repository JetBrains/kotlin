package kotlin.properties


/**
 * Base interface that can be used for implementing property delegates of read-only properties.
 *
 * This is provided only for convenience; you don't have to extend this interface
 * as long as your property delegate has methods with the same signatures.
 *
 * @param R the type of object which owns the delegated property.
 * @param T the type of the property value.
 */
public interface ReadOnlyProperty<in R, out T> {
    /**
     * Returns the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param property the metadata for the property.
     * @return the property value.
     */
    public fun getValue(thisRef: R, property: PropertyMetadata): T = get(thisRef, property)

    //TODO drop after bootstrap
    @Deprecated("Use getValue() instead.", ReplaceWith("getValue(thisRef, property)"))
    public fun get(thisRef: R, property: PropertyMetadata): T = getValue(thisRef, property)
}

/**
 * Base interface that can be used for implementing property delegates of read-write properties.
 *
 * This is provided only for convenience; you don't have to extend this interface
 * as long as your property delegate has methods with the same signatures.
 *
 * @param R the type of object which owns the delegated property.
 * @param T the type of the property value.
 */
public interface ReadWriteProperty<in R, T> {
    /**
     * Returns the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param property the metadata for the property.
     * @return the property value.
     */
    public fun getValue(thisRef: R, property: PropertyMetadata): T = get(thisRef, property)

    //TODO drop after bootstrap
    @Deprecated("Use getValue() instead.", ReplaceWith("getValue(thisRef, property)"))
    public fun get(thisRef: R, property: PropertyMetadata): T = getValue(thisRef, property)

    /**
     * Sets the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param property the metadata for the property.
     * @param value the value to set.
     */
    public fun setValue(thisRef: R, property: PropertyMetadata, value: T) {
        set(thisRef, property, value)
    }

    //TODO drop after bootstrap
    @Deprecated("Use setValue() instead.", ReplaceWith("setValue(thisRef, property, value)"))
    public fun set(thisRef: R, property: PropertyMetadata, value: T) = setValue(thisRef, property, value)
}
