/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory

enum class WrongEntityTypeTarget {
    NOT_CLASS,
    ABSTRACT,
    NOT_ENTITY,
    NO_CONSTRUCTOR,
    ALREADY_EXTENDS,
}

enum class WrongAtttributeTarget {
    NOT_REF,
    NOT_ENTITY,
    NO_SERIALIZER,
    NO_ENTITY_TYPE,
    DUPLICATED_ATTRIBUTE,
}

object RhizomedbFirErrors {
    val MANY_NON_SET by error0<PsiElement>()
    val NON_ATTRIBUTE by error0<PsiElement>()

    val WRONG_ENTITY_TYPE_TARGET by error1<PsiElement, WrongEntityTypeTarget>()
    val WRONG_ATTRIBUTE_TARGET by error1<PsiElement, WrongAtttributeTarget>()

    init {
        RootDiagnosticRendererFactory.registerFactory(RhizomedbDefaultErrorMessages)
    }
}

private object RhizomedbDefaultErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("Rhizomedb").apply {
        put(
            RhizomedbFirErrors.MANY_NON_SET,
            "Property marked with @Many should be a Set",
        )

        put(
            RhizomedbFirErrors.NON_ATTRIBUTE,
            "Property marked with @Many should be marked with attribute annotation",
        )

        put(
            RhizomedbFirErrors.WRONG_ENTITY_TYPE_TARGET,
            "EntityType generation not possible: {0}",
            RhizomedbDiagnosticRenderers.WRONG_ENTITY_TYPE_TARGET_RENDERER,
        )

        put(
            RhizomedbFirErrors.WRONG_ATTRIBUTE_TARGET,
            "Attribute generation not possible: {0}",
            RhizomedbDiagnosticRenderers.WRONG_ATTRIBUTE_TARGET_RENDERER,
        )
    }
}

private object RhizomedbDiagnosticRenderers {
    val WRONG_ENTITY_TYPE_TARGET_RENDERER = Renderer<WrongEntityTypeTarget> { target ->
        when (target) {
            WrongEntityTypeTarget.NOT_CLASS -> "target should be a regular class"
            WrongEntityTypeTarget.ABSTRACT -> "target cannot be abstract"
            WrongEntityTypeTarget.NOT_ENTITY -> "target should be an Entity"
            WrongEntityTypeTarget.NO_CONSTRUCTOR -> "target should have from EID constructor"
            WrongEntityTypeTarget.ALREADY_EXTENDS -> "target companion already extends a class"
        }
    }

    val WRONG_ATTRIBUTE_TARGET_RENDERER = Renderer<WrongAtttributeTarget> { target ->
        when (target) {
            WrongAtttributeTarget.NOT_REF -> "Entity property can be marked with @RefAttribute only"
            WrongAtttributeTarget.NOT_ENTITY -> "reference attribute should be an Entity"
            WrongAtttributeTarget.NO_SERIALIZER -> "value attribute should have serializer"
            WrongAtttributeTarget.NO_ENTITY_TYPE -> "no EntityType as companion found"
            WrongAtttributeTarget.DUPLICATED_ATTRIBUTE -> "property may be marked with single attribute annotation only"
        }
    }
}