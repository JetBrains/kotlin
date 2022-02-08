/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.inference

/**
 * The value of a binding. If [token] is not null, the binding is bound to [token]. [size] is the
 * number of [Binding] instances that share this value. This is used to optimize unifying open
 * bindings. [index] is not used directly but makes debugging easier.
 */
class Value(var token: String?, var observers: Set<Bindings>) {
    var size: Int = 1
    val index = valueIndex++
}

private var valueIndex = 0

/**
 * A binding that is either closed (with a non-null [token]) or open. Unified bindings are linked
 * together in a circular list by [Bindings]. All linked bindings are all closed simultaneously
 * when anyone of them is unified to a closed binding.
 *
 * @param token the applier token the binding is bound to if it is closed.
 */
class Binding(token: String? = null, observers: Set<Bindings>) {
    /**
     * The token that is bound to this binding. If [token] is null then the binding is still open.
     */
    val token: String? get() = value.token

    /**
     * The value of the binding. All linked bindings share the same value which also maintains
     * the count of linked bindings.
     */
    var value: Value = Value(token, observers)

    /**
     * The linked list next pointer. The list is circular an always non-empty as a binding will
     * always at least contain itself in its own list. All linked [Binding] are in the same
     * circular list. Open bindings that are unified together are linked.
     */
    var next = this

    override fun toString(): String {
        return value.token?.let { "Binding(token = $it)" } ?: "Binding(${value.index})"
    }
}

/**
 * [Bindings] can create open or closed bindings and can unify bindings together. A binding is
 * either bound to an applier token or it is open. All open bindings of the same value are linked
 * together in a circular list. When variables from different groups are unified, their lists are
 * merged and they are all given the lower of the two group's value. Bindings of different values
 * with the same token are considered unified but there is no need to link them as neither will
 * ever change.
 */
class Bindings {
    private val listeners = mutableListOf<() -> Unit>()

    /**
     * Create a fresh open applier binding variable
     */
    fun open() = Binding(observers = setOf(this))

    /**
     * Create a closed applier binding variable
     */
    fun closed(target: String) = Binding(token = target, emptySet())

    /**
     * Listen for when a unification closed a binding or bound two binding groups together.
     */
    fun onChange(callback: () -> Unit): () -> Unit {
        listeners.add(callback)
        return {
            listeners.remove(callback)
        }
    }

    /**
     * Unify a and b; returns true if the unification succeeded. If both a and b are unbound they
     * will be bound together and will simultaneously be bound if either is later bound. If only
     * one is bound the other will be bound to the bound token. If a and b are bound already,
     * unify() returns true if they are bound to the same token or false if they are not. Binding
     * two open bindings that are already bound together is a noop and succeeds.
     *
     * @param a an applier binding variable
     * @param b an applier binding variable
     * @return true if [a] and [b] can be unified together.
     */
    fun unify(a: Binding, b: Binding): Boolean {
        val at = a.value.token
        val bt = b.value.token
        return when {
            at != null && bt == null -> bind(b, at)
            at == null && bt != null -> bind(a, bt)
            at != null && bt != null -> at == bt
            else -> bind(a, b)
        }
    }

    private fun unifyValues(b: Binding, value: Value) {
        b.value = value
        var current = b.next
        while (current != b) {
            current.value = value
            current = current.next
        }
    }

    private fun bind(a: Binding, b: Binding): Boolean {
        // Update the smallest binding list. If the bindings already have the same value then
        // they are already bound together. Using the smallest list ensures that binding all
        // bindings together will be no worse than O(N log N) operations where N is the number of
        // bindings.
        val aValue = a.value
        val bValue = b.value
        if (aValue == bValue) return true
        val aValueSize = aValue.size
        val bValueSize = bValue.size
        val newObservers = aValue.observers + bValue.observers
        if (aValueSize > bValueSize) {
            aValue.size += bValueSize
            aValue.observers = newObservers
            unifyValues(b, aValue)
        } else {
            bValue.size += aValueSize
            bValue.observers = newObservers
            unifyValues(a, bValue)
        }

        // Merge the circular lists by swapping a and b's next pointers
        //   https://en.wikipedia.org/wiki/Linked_list#Circularly_linked_vs._linearly_linked.
        // This only works if a and b are in distinct lists. If they are in the same list this
        // breaks the list apart instead of merging. To ensure the lists are distinct the values
        // of merged lists are made identical and all new nodes are given unique values. This
        // ensures that the bindings in the same list have ths same value and the `aValue ==
        // bValue` check above prevent list splits.
        val nextA = a.next
        val nextB = b.next
        a.next = nextB
        b.next = nextA
        bindingValueChanged(a.value)
        return true
    }

    // Bind the binding to a token. It binds all bindings in the same list to the token.
    private fun bind(binding: Binding, token: String): Boolean {
        val value = binding.value
        value.token = token
        bindingValueChanged(value)
        value.observers = emptySet()
        return true
    }

    private fun bindingValueChanged(value: Value) {
        for (observer in value.observers) {
            observer.changed()
        }
    }

    private fun changed() {
        if (listeners.isNotEmpty()) {
            // Enumerate a copy of the list to allow listeners to delete themselves from the list.
            for (listener in listeners.toMutableList())
                listener()
        }
    }
}