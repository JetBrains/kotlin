/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

/**
 * A signature of JVM method or field
 *
 * @property name name of method or field
 * @property desc JVM descriptor of a method, e.g. `(Ljava/lang/Object;)Z`, or a field type, e.g. `Ljava/lang/String;`
 */
data class JvmMemberSignature(val name: String, val desc: String) {

    /**
     * Returns `true` when this signature represents a field.
     */
    val isField: Boolean get() = desc.indexOf("(") < 0

    /**
     * Returns `true` when this signature represents a method.
     */
    val isMethod: Boolean get() = desc.indexOf("(") >= 0

    override fun toString() = if (isField) name + ":" + desc else name + desc
}

internal fun org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature.wrapAsPublic() = JvmMemberSignature(name, desc)