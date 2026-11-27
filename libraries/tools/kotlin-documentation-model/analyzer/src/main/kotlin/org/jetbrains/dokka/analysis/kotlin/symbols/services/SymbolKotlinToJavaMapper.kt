/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import org.jetbrains.dokka.analysis.kotlin.internal.KotlinToJavaService
import org.jetbrains.kotlin.documentation.analyzer.ids.classIdFromDeclarationId
import org.jetbrains.kotlin.documentation.analyzer.ids.escape
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap // see https://github.com/Kotlin/dokka/issues/3226
import org.jetbrains.kotlin.documentation.KdElementId

/**
 * This is copy-pasted from org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl.DescriptorKotlinToJavaMapper
 */
internal class SymbolKotlinToJavaMapper : KotlinToJavaService {

    override fun findAsJava(kotlinDeclarationId: KdElementId): KdElementId? {
        val classId = classIdFromDeclarationId(kotlinDeclarationId)
        return JavaToKotlinClassMap.mapKotlinToJava(classId.asSingleFqName().toUnsafe())?.let { KdElementId(it.escape()) }
    }
}
