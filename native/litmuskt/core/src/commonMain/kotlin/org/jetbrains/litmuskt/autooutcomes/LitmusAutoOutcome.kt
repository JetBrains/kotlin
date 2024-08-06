package org.jetbrains.litmuskt.autooutcomes

import org.jetbrains.litmuskt.LitmusOutcome

/**
 * A convenience interface to simplify specifying outcomes.
 *
 * All classes implementing this interface provide some r1, r2, ... variables
 * to write the outcome into. If a litmus test's state extends one of these classes,
 * specifying `outcome { ... }` is not necessary, as it will be inferred from r1, r2, ...
 *
 * Children classes should override `hashCode()` and `equals()` so that they are compared
 * based on their outcome only. They should also override `toString()` so that they only display
 * their outcome when printed. For these reasons the functions are overridden in this
 * interface such that their implementation is forced in children.
 *
 * These classes are also used as outcomes themselves in order to better utilize resources.
 */
sealed interface LitmusAutoOutcome {
    override fun toString(): String
    override fun hashCode(): Int

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun equals(o: Any?): Boolean

    // for JCStress interop
    fun toList(): List<LitmusOutcome>
    fun parseOutcome(str: String): LitmusAutoOutcome
}

