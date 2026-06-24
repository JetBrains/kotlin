/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.java.FirUnresolvedJavaClassNamesProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class LombokUnresolvedJavaClassNamesProvider(session: FirSession) :
    FirExtensionSessionComponent(session),
    FirUnresolvedJavaClassNamesProvider
{
    private val registry = ConcurrentHashMap<Name, ClassId>()

    override fun findClassIdBySimpleName(simpleName: Name): ClassId? = registry[simpleName]

    override fun registerSimpleNameAlias(simpleName: Name, classId: ClassId) {
        registry.putIfAbsent(simpleName, classId)
    }

    @Suppress("UNCHECKED_CAST")
    override val componentClass: KClass<out FirExtensionSessionComponent>
        get() = FirUnresolvedJavaClassNamesProvider::class as KClass<out FirExtensionSessionComponent>

    companion object {
        val Factory: FirExtensionSessionComponent.Factory = FirExtensionSessionComponent.Factory {
            LombokUnresolvedJavaClassNamesProvider(it)
        }
    }
}
