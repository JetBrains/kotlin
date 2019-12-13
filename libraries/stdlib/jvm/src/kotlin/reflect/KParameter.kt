/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.reflect

/**
 * Represents a parameter passed to a function or a property getter/setter,
 * including `this` and extension receiver parameters.
 */
public interface KParameter : KAnnotatedElement {
    /**
     * 0-based index of this parameter in the parameter list of its containing callable.
     */
    public val index: Int

    /**
     * Name of this parameter as it was declared in the source code,
     * or `null` if the parameter has no name or its name is not available at runtime.
     * Examples of nameless parameters include `this` instance for member functions,
     * extension receiver for extension functions or properties, parameters of Java methods
     * compiled without the debug information, and others.
     */
    public val name: String?

    /**
     * Type of this parameter. For a `vararg` parameter, this is the type of the corresponding array,
     * not the individual element.
     */
    public val type: KType

    /**
     * Kind of this parameter.
     */
    public val kind: Kind

    /**
     * Kind represents a particular position of the parameter declaration in the source code,
     * such as an instance, an extension receiver parameter or a value parameter.
     */
    public enum class Kind {
        /** Instance required to make a call to the member, or an outer class instance for an inner class constructor. */
        INSTANCE,

        /** Extension receiver of an extension function or property. */
        EXTENSION_RECEIVER,

        /** Ordinary named value parameter. */
        VALUE,
    }

    /**
     * `true` if this parameter is optional and can be omitted when making a call via [KCallable.callBy], or `false` otherwise.
     *
     * A parameter is optional in any of the two cases:
     * 1. The default value is provided at the declaration of this parameter.
     * 2. The parameter is declared in a member function and one of the corresponding parameters in the super functions is optional.
     */
    public val isOptional: Boolean

    /**
     * `true` if this parameter is `vararg`.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/functions.html#variable-number-of-arguments-varargs)
     * for more information.
     */
    @SinceKotlin("1.1")
    public val isVararg: Boolean
}
