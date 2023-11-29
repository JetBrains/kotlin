/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.jvm

import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature as JvmMemberSignatureImpl

/**
 * A signature of a JVM method or field.
 *
 * @property name name of method or field
 * @property descriptor JVM descriptor of a method, e.g. `(Ljava/lang/Object;)Z`, or a field type, e.g. `Ljava/lang/String;`
 */
public sealed class JvmMemberSignature {

    public abstract val name: String
    public abstract val descriptor: String

    /**
     * Returns a string representation of the signature.
     *
     * In case of a method it's just [name] and [descriptor] concatenated together, e.g. `equals(Ljava/lang/Object;)Z`
     *
     * In case of a field [name] and [descriptor] are concatenated with `:` separator, e.g. `value:Ljava/lang/String;`
     */
    abstract override fun toString(): String

    // Two following declarations are deprecated since 0.6.1, should be error in 0.7.0+

    @Deprecated("Deprecated for removal. Use descriptor instead", ReplaceWith("descriptor"), level = DeprecationLevel.ERROR)
    public val desc: String get() = descriptor

    @Deprecated(
        "asString() is deprecated as redundant. Use toString() instead",
        ReplaceWith("toString()"),
        level = DeprecationLevel.ERROR
    )
    public fun asString(): String = toString()
}

/**
 * A signature of a JVM method in the JVM-based format.
 *
 * Example: `JvmMethodSignature("equals", "(Ljava/lang/Object;)Z")`.
 *
 * @see JvmMemberSignature
 */
public data class JvmMethodSignature(override val name: String, override val descriptor: String) : JvmMemberSignature() {
    override fun toString(): String = name + descriptor
}

/**
 * A signature of a JVM field in the JVM-based format.
 *
 * Example: `JvmFieldSignature("value", "Ljava/lang/String;")`.
 *
 * @see JvmMemberSignature
 */
public data class JvmFieldSignature(override val name: String, override val descriptor: String) : JvmMemberSignature() {
    override fun toString(): String = "$name:$descriptor"
}


internal fun JvmMemberSignatureImpl.Method.wrapAsPublic() = JvmMethodSignature(name, desc)
internal fun JvmMemberSignatureImpl.Field.wrapAsPublic() = JvmFieldSignature(name, desc)
