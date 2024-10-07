/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.repl.k2

import org.jetbrains.kotlin.ir.types.IdSignatureValues.result
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.script.experimental.api.ResultValue

/**
 * This class represents the entire REPL state and is used to track all top-level properties as well as
 * cell output.
 *
 * The basic design principel is that all REPL classes should be _stateless_, and if they need to store
 * or fetch state, it should happen through this class.
 *
 * This class is very much work-in-progress, and is mostly here to validate concepts rather
 * than making sure that all edge cases are covered.
 *
 * Design Questions:
 * - Naming: Currently using `$<X>` to denotate "internal" params, and it should also prevent naming clashes.
 *   Is this how we want to do that?
 * - Does creating properties need to be thread safe since we should only create top-level properties.
 * - Current state is not thread-safe. Is there a world where we will be running cells in parallel on the same state?
 * - Do we need some kind of map between cellId and output?
 * - Should we have a single ReplState for the entire script or a chain of ReplStates, i.e. one for each cell?
 * - Inside the same cell, we could in theory just use the standard compiler logic for getters and setters (less work?)
 *   but that would make it different for getters in same cell vs. other cell. How big of a problem is that?
 * - We need a way to indicate if a property has a backing field or not. Right now there are multiple methods.
 *   Is there a better way? Would a `hasBackingField` parameter be nicer?
 * - `thisRef` property in custom getters/setters are going to be `ReplState`. It is either that or the snippet class
 *   which is also an implementation detail. Unclear what would be most appropriate. Probably we just need to choose
 *   and document it.
 * - How do we map line numbers between the two paradigmes, especially when it comes to getters/setters?
 */

/**
 * Regarding getters/setters:
 * We could inline a lot of this, but it would mean making the backingField map public. We should measure the
 * performance trade-off here.
 *
 *  inline fun <V> setReadWriteProperty(
 *         property: KProperty<*>,
 *         initialValue: V?,
 *         crossinline getter: (state: ReplState, name: String) -> V? = { state, name -> state.getBackingFieldValue<V>(name)},
 *         crossinline setter: (state: ReplState, name: String, value: V) -> Unit = { state, name, value -> state.setBackingFieldValue(name, value) }
 *     ): ReadWriteProperty<Any?, V> {
 */

private typealias GetterFunc<V> = (state: ReplState, name: String) -> V
private typealias SetterFunc<V> = (state: ReplState, name: String, value: V) -> Unit

class ReplState {

    // Data class wrapping the getter for a script property.
    private data class Getter<V>(
        val property: KProperty<*>,
        val func: GetterFunc<V>,
    )

    // Data class wrapping the setter for a script property.
    private data class Setter<V>(
        val property: KProperty<*>,
        val func: (ReplState, KProperty<*>, V) -> Unit
    )

    // Track script properties and output
    private val backingFieldValues: MutableMap<String, Any?> = mutableMapOf()
    private val getters: MutableMap<String, Getter<*>> = mutableMapOf()
    private val setters: MutableMap<String, Setter<*>> = mutableMapOf()
    private val outputs: MutableList<ResultValue> = mutableListOf()

    /**
     * Sets the value of the backing field for a named property
     */
    fun setBackingFieldValue(name: String, value: Any?) {
        backingFieldValues[name] = value
    }

    /**
     * Checks if a backing field exists for a given property.
     */
    fun hasBackingField(name: String): Boolean {
        return backingFieldValues.containsKey(name)
    }

    /**
     * Returns the backing field value for the given property.
     * If no backing field exists, an [IllegalStateException] is thrown.
     *
     * Use [hasBackingField] to check if calling this function is valid.
     */
    fun <V> getBackingFieldValue(property: String): V {
        @Suppress("UNCHECKED_CAST")
        return backingFieldValues[property] as V
    }

    /**
     * If the evaluation threw an error, store the result here.
     */
    fun setErrorOutput(error: ResultValue.Error) {
        outputs.add(error)
    }

    /**
     * Set the output of the current cell to `Unit`, i.e., the cell
     * was evaluated successfully, but has not output.
     */
    fun setUnitOutput(scriptInstance: Any) {
        outputs.add(
            ResultValue.Unit(
                scriptClass = scriptInstance::class,
                scriptInstance = scriptInstance
            )
        )
    }

    /**
     * Save the output of the current snippet, this can either be Unit or an actual value
     */
    fun setValueOutput(scriptInstance: Any, value: Any?) {
        // TODO We should probably rethink the API of `ResultValue` here
        outputs.add(
            ResultValue.Value(
                name = "out",
                value = value,
                type = if (value != null) value::class.qualifiedName.toString() else "Any?",
                scriptClass = scriptInstance::class,
                scriptInstance = scriptInstance,
            )
        )
    }

    /**
     * Returns the [ResultValue] output from the last evaluated snippet.
     * [ResultValue.NotEvaluated] is returned if not cells has been evaluated.
     */
    fun getLastOutput(): ResultValue {
        return if (outputs.isNotEmpty()) {
            outputs.last()
        } else {
            ResultValue.NotEvaluated
        }
    }

    /**
     * Returns all outputs from evaluated snippets.
     */
    fun getOutputHistory(): List<Any?> {
        return outputs
    }

    /**
     * Creates a read-only property, i.e. `val` property that has a backing field.
     *
     * Example:
     * ```
     * val field1 = "Hello"
     * ```
     */
    fun <V> createReadOnlyProperty(
        property: KProperty<*>,
        initialValue: V,
        getter: GetterFunc<V> = { state, name -> state.getBackingFieldValue<V>(name) }
    ): ReadOnlyProperty<Any?, V> {
        backingFieldValues[property.name] = initialValue
        getters[property.name] = Getter(property, getter)

        // Return dummy property (for now it does the right thing), but will never be called
        return ReadOnlyProperty { thisRef: Any?, property: KProperty<*> ->
            TODO()
            // getters[property.name]!!.func.invoke(this, property) as V
        }
    }

    /**
     * Creates a read-only property, i.e. `val` property that does not have a backing field.
     *
     * Example:
     * ```
     * val field1: String
     *   get() { return "Hello" }
     * ```
     */
    fun <V> createReadOnlyProperty(
        property: KProperty<*>,
        getter: GetterFunc<V> = { state, name -> state.getBackingFieldValue(name) }
    ): ReadOnlyProperty<Any?, V> {
        getters[property.name] = Getter(property) { state: ReplState, name: String ->
            getter(state, property.name)
        }
        return ReadOnlyProperty { thisRef: Any?, property: KProperty<*> ->
            TODO("Should not be called")
            // getters[property.name] as V
        }
    }

    /**
     * Creates a read-write property, i.e., a `var` property that also has a backing field.
     *
     * Example:
     * ```
     * var field1 = "Hello"
     * var field2: String
     *   get() { return "From: $field" }
     * ```
     */
    fun <V> createReadWriteProperty(
        property: KProperty<*>,
        initialValue: V,
        getter: GetterFunc<V> = { state: ReplState, name: String -> state.getBackingFieldValue(name) },
        setter: SetterFunc<V> = { state, name, value: V -> state.setBackingFieldValue(name, value) }
    ): ReadWriteProperty<Any?, V> {
        // Store property state
        backingFieldValues[property.name] = initialValue
        getters[property.name] = Getter(property) {  state: ReplState, name: String ->
            getter(state, name)
        }
        setters[property.name] = Setter(property) { state: ReplState, prop: KProperty<*>, value: V ->
            setter(state, prop.name, value)
        }

        // Return an accessor that
        return object: ReadWriteProperty<Any?, V> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): V {
                TODO("Do we need an implementation here?")
                // return getters[property.name] as V
            }
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
                TODO("Do we need an implementation here?")
                // return (setters[property.name]!! as Setter<V>).func.invoke(this@ReplState, property, value)
            }
        }
    }

    /**
     * Creates a read-write property, i.e., a `var` property that does not have a backing field
     *
     * Example:
     * ```
     * var field1: String
     *   get() { return callMethod() }
     *   set(value) { callMethod(value) }
     * ```
     */
    fun <V> createReadWriteProperty(
        property: KProperty<*>,
        getter: GetterFunc<V> = { state: ReplState, name: String -> state.getBackingFieldValue<V>(name) },
        setter: SetterFunc<V> = { state, name, value -> state.setBackingFieldValue(name, value) }
    ): ReadWriteProperty<Any?, V> {
        // Store property state
        getters[property.name] = Getter(property) {  state: ReplState, name: String ->
            getter(state, name)
        }
        setters[property.name] = Setter(property) { state: ReplState, prop: KProperty<*>, value: V ->
            setter(state, prop.name, value)
        }

        // Return an accessor that
        return object: ReadWriteProperty<Any?, V> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): V {
                TODO("Do we need an implementation here?")
                // return getters[property.name] as V
            }
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
                TODO("Do we need an implementation here?")
                // return (setters[property.name]!! as Setter<V>).func.invoke(this@ReplState, property, value)
            }
        }
    }

    /**
     * Creates a property that is being generated by a delegate, i.e. using the `by` keyword.
     *
     * Example:
     * ```
     * var field1 by Delegates.observable("Initial") { prop, old, new ->
     *     println("Property ${prop.name} changed from '$old' to '$new'")
     * }
     * ```
     */
    fun <V> createDelegateProperty(prop: KProperty<V>, delegate: ReadWriteProperty<Any?, V>): ReadWriteProperty<Any?, V> {
        getters[prop.name] = Getter(prop) {  state: ReplState, name: String ->
            delegate.getValue(this, prop)
        }
        setters[prop.name] = Setter(prop) { state: ReplState, prop: KProperty<*>, value: V ->
            delegate.setValue(this, prop, value)
        }
        return object: ReadWriteProperty<Any?, V> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): V {
                TODO("Do we need an implementation here?")
                // return getters[prop.name]!!.func.invoke(this@ReplState, prop.name) as V
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
                TODO("Do we need an implementation here?")
                // (setters[prop.name]!! as Setter<V>).func.invoke(this@ReplState, property, value)
            }
        }
    }

    /**
     * Create a lazy property.
     *
     * Example:
     * ```
     * val field1 by lazy { "Hello" }
     * ```
     */
    fun <V> createLazyProperty(prop: KProperty<*>, lazy: Lazy<V>): ReadOnlyProperty<Any?, V> {
        getters[prop.name] = Getter(prop) {  state: ReplState, name: String ->
            lazy.getValue(this, prop)
        }
        return ReadOnlyProperty<Any?, V> { thisRef, property ->
            TODO("Do we need an implementation here?")
            //  return getters[prop.name]!!.func.invoke(this@ReplState, prop.name) as V
        }
    }

    /**
     * Returns the value of property [name] using its getter.
     */
    fun <V> getPropertyValue(name: String): V {
        val getter = getters[name] ?: throw IllegalStateException("Could not find getter for: $name")
        @Suppress("UNCHECKED_CAST")
        return getter.func.invoke(this, name) as V
    }

    /**
     * Set the value of property [name] using its setter.
     */
    fun <V> setPropertyValue(name: String, value: V) {
        @Suppress("UNCHECKED_CAST")
        val setter = (setters[name] ?: throw IllegalStateException("Could not find setter for: $name")) as Setter<V>
        return setter.func.invoke(this, setter.property, value)
    }
}
