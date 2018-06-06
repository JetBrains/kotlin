/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature as JvmMemberSignatureImpl

/**
 * A signature of a JVM method or field.
 *
 * @property name name of method or field
 * @property desc JVM descriptor of a method, e.g. `(Ljava/lang/Object;)Z`, or a field type, e.g. `Ljava/lang/String;`
 */
sealed class JvmMemberSignature {

    abstract val name: String
    abstract val desc: String

    /**
     * Returns a string representation of the signature.
     *
     * In case of a method it's just [name] and [desc] concatenated together, e.g. `equals(Ljava/lang/Object;)Z`
     *
     * In case of a field [name] and [desc] are concatenated with `:` separator, e.g. `value:Ljava/lang/String;`
     */
    abstract fun asString(): String

    final override fun toString() = asString()
}

/**
 * A signature of a JVM method.
 *
 * @see JvmMemberSignature
 */
data class JvmMethodSignature(override val name: String, override val desc: String) : JvmMemberSignature() {
    override fun asString() = name + desc
}

/**
 * A signature of a JVM field.
 *
 * @see JvmMemberSignature
 */
data class JvmFieldSignature(override val name: String, override val desc: String) : JvmMemberSignature() {
    override fun asString() = name + ":" + desc
}


internal fun JvmMemberSignatureImpl.Method.wrapAsPublic() = JvmMethodSignature(name, desc)
internal fun JvmMemberSignatureImpl.Field.wrapAsPublic() = JvmFieldSignature(name, desc)