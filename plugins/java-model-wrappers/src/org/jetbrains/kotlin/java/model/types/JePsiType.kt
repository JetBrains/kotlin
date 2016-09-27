/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.java.model.types

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.annotation.processing.impl.toDisposable
import org.jetbrains.kotlin.java.model.internal.JeElementRegistry
import org.jetbrains.kotlin.annotation.processing.impl.dispose

interface JePsiType : JeTypeMirror {
    val psiType: PsiType
}

abstract class JePsiTypeBase<out T : PsiType>(
        psiType: T,
        manager: PsiManager
) : JePsiType, JeTypeWithManager, Disposable {
    private val disposableManager = manager.toDisposable()
    private val disposableType = psiType.toDisposable()

    init {
        ServiceManager.getService(manager.project, JeElementRegistry::class.java).register(this)
    }

    override fun dispose() = dispose(disposableManager, disposableType)

    override val psiManager: PsiManager
        get() = disposableManager()

    override val psiType: T
        get() = disposableType()
}