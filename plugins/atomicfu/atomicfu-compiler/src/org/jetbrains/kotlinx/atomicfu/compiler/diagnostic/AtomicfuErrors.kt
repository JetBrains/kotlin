/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.PositioningStrategyKinds
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer

object AtomicfuErrors : KtDiagnosticsContainer() {
    val PUBLIC_ATOMICS_ARE_FORBIDDEN by error1<String>(PositioningStrategyKinds.VISIBILITY_MODIFIER)
    val PUBLISHED_API_ATOMICS_ARE_FORBIDDEN by error1<String>(PositioningStrategyKinds.VISIBILITY_MODIFIER)
    val ATOMIC_PROPERTIES_SHOULD_BE_VAL by error1<String>(PositioningStrategyKinds.VAL_OR_VAR_NODE)
    val NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN by error0(PositioningStrategyKinds.VISIBILITY_MODIFIER)
    val ATOMIC_FUNCTION_CALLABLE_REFERENCES_ARE_FORBIDDEN by error0(PositioningStrategyKinds.REFERENCE_BY_QUALIFIED)
    val ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY by error0(PositioningStrategyKinds.DEFAULT)
    val ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD by error0(PositioningStrategyKinds.VAL_OR_VAR_NODE)
    val ATOMIC_LOCALS_ARE_FORBIDDEN by error0(PositioningStrategyKinds.VAL_OR_VAR_NODE)
    val LOCAL_PROPERTIES_DELEGATED_TO_ATOMICS_ARE_FORBIDDEN by error0(PositioningStrategyKinds.VAL_OR_VAR_NODE)
    val ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN by error0(PositioningStrategyKinds.DECLARATION_NAME)
    val ATOMIC_EXTENSION_MUST_BE_NON_PUBLIC_INLINE by error0(PositioningStrategyKinds.DECLARATION_NAME)
    val ATOMIC_ARRAY_EXTENSION_PROPERTIES_ARE_FORBIDDEN by error0(PositioningStrategyKinds.DECLARATION_NAME)
    val NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN by error0(PositioningStrategyKinds.DECLARATION_RETURN_TYPE)
    val ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN by error0(PositioningStrategyKinds.DEFAULT)
    val ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION by error0(PositioningStrategyKinds.DEFAULT)
    val ATOMIC_DOES_NOT_INHERIT_FUNCTIONS_FROM_ANY by error0(PositioningStrategyKinds.CALL_ELEMENT_WITH_DOT)
    val PROPERTIES_DELEGATED_TO_ATOMICS_MUST_BE_IN_SAME_CONTAINER by error2<String, String>(PositioningStrategyKinds.DECLARATION_NAME)
    val INSTANCE_PROPERTIES_DELEGATED_TO_ATOMICS_CAN_ONLY_ACCESS_SAME_INSTANCE by error1<String>(PositioningStrategyKinds.DECLARATION_NAME)
    val COMPANION_PROPERTIES_CAN_ONLY_DELEGATE_TO_ATOMICS_IN_COMPANION_BLOCK by error0(PositioningStrategyKinds.DECLARATION_NAME)
    override fun getRendererFactory(): BaseDiagnosticRendererFactory = AtomicfuErrorMessages
}
