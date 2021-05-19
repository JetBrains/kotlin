/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.internal.visitor

import kotlin.reflect.KClass
import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.IdentifiableMember
import kotlin.script.experimental.typeProviders.generatedCode.internal.visit

// Imports

internal fun GeneratedCodeVisitor.useImports(imports: Iterable<String>) {
    imports.forEach(::useImport)
}

@JvmName("useImportsWithIdentifiableMemberIterable")
internal fun GeneratedCodeVisitor.useImports(identifiableMembers: Iterable<IdentifiableMember>) {
    for (member in identifiableMembers) {
        useImports(member.imports())
    }
}

internal fun GeneratedCodeVisitor.useImports(vararg identifiableMembers: IdentifiableMember) = useImports(identifiableMembers.asIterable())

internal fun GeneratedCodeVisitor.useImport(kClass: KClass<*>) = useImport(kClass.qualifiedName!!)

@JvmName("useImportsWithClassIterable")
internal fun GeneratedCodeVisitor.useImports(classes: Iterable<KClass<*>>) = classes.forEach { useImport(it) }

internal fun GeneratedCodeVisitor.useImports(vararg classes: KClass<*>) = classes.forEach { useImport(it) }
