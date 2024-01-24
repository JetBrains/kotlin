/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.analysis.kotlin.internal.KotlinToJavaService
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap // or import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap see https://github.com/Kotlin/dokka/issues/3226
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * This is copy-pasted from org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl.DescriptorKotlinToJavaMapper
 */
internal class SymbolKotlinToJavaMapper : KotlinToJavaService {

    override fun findAsJava(kotlinDri: DRI): DRI? {
        return kotlinDri.partialFqName().mapToJava()?.toDRI(kotlinDri)
    }

    private fun DRI.partialFqName() = packageName?.let { "$it." } + classNames

    private fun String.mapToJava(): ClassId? =
        JavaToKotlinClassMap.mapKotlinToJava(FqName(this).toUnsafe())

    private fun ClassId.toDRI(dri: DRI?): DRI = DRI(
        packageName = packageFqName.asString(),
        classNames = classNames(),
        callable = dri?.callable,//?.asJava(), TODO: check this
        extra = null,
        target = PointingToDeclaration
    )

    private fun ClassId.classNames(): String =
        shortClassName.identifier + (outerClassId?.classNames()?.let { ".$it" } ?: "")
}
