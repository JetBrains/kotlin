/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.repl.k2

import kotlin.reflect.*
import kotlin.reflect.full.createType

interface ExecutableReplSnippet {
    /**
     * Evaluates this snippet based on the current state of the REPL.
     */
    suspend fun evaluate(`$replState`: ReplState)
}

/**
 * Just a helper function for now that makes it easier to create a mock property.
 * This should be replaced by the compiler creating this output in the final
 * implementation.
 */
inline fun <reified T> createMockProperty(name: String): KProperty<T> {
    return object: KProperty<T> {
        override val annotations: List<Annotation> = emptyList()
        override val getter: KProperty.Getter<T> get() = TODO()
        override val isAbstract: Boolean = false
        override val isConst: Boolean = false
        override val isFinal: Boolean = false
        override val isLateinit: Boolean = false
        override val isOpen: Boolean = false
        override val isSuspend: Boolean = false
        override val name: String = name
        override val parameters: List<KParameter> = emptyList()
        override val returnType: KType get() = T::class.createType()
        override val typeParameters: List<KTypeParameter> = emptyList()
        override val visibility: KVisibility? = null
        override fun call(vararg args: Any?): T { TODO() }
        override fun callBy(args: Map<KParameter, Any?>): T { TODO() }
    }
}
