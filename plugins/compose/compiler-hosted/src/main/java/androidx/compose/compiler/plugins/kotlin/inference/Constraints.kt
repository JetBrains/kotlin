/*
 * Copyright 2026 The Android Open Source Project
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
 * Represents constraints that allow a certain (possibly infinite) set of tokens.
 */
class Constraints private constructor(private val allowed: Set<String>? = null) {
    val allowsAllTokens: Boolean get() = allowed == null

    val blocksAllTokens: Boolean get() = allowed?.isEmpty() == true

    /**
     * Whether these constraints allow exactly one token.
     */
    val allowsSingleToken: Boolean get() = allowed?.size == 1

    /**
     * The set of tokens allowed by these constraints. If these constraints allow all tokens, an
     * [IllegalStateException] will be thrown.
     */
    val allowedTokens: Set<String>
        get() =
            allowed ?: throw IllegalStateException("Cannot be accessed on an instance that allows all tokens")

    fun allows(token: String): Boolean = allowed?.contains(token) ?: true

    infix fun intersect(other: Constraints): Constraints = when {
        this.allowsAllTokens -> other
        other.allowsAllTokens -> this
        else -> Constraints(this.allowed!! intersect other.allowed!!)
    }

    override fun equals(other: Any?) = other is Constraints && other.allowed == allowed

    override fun hashCode() = allowed.hashCode()

    override fun toString(): String =
        allowed?.joinToString(separator = ", ", prefix = "{", postfix = "}") ?: "unrestricted"

    companion object {
        /**
         * Constraints that allow all tokens.
         */
        val UNRESTRICTED = Constraints()

        /**
         * Returns constraints that only allow the tokens in [allowed].
         */
        fun restrictedTo(allowed: Set<String>) = Constraints(allowed)
    }
}
