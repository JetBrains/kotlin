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

package org.jetbrains.kotlin.java.model

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.annotation.processing.impl.toDisposable
import org.jetbrains.kotlin.java.model.internal.JeElementRegistry
import javax.lang.model.element.Element

interface JePsiElementOwner {
    val psi: PsiElement
}

interface JeElement : Element, JePsiElementOwner

abstract class JeDisposablePsiElementOwner<out T : PsiElement>(element: T) : JePsiElementOwner, Disposable {
    private val disposablePsi = element.toDisposable()

    init {
        @Suppress("LeakingThis")
        ServiceManager.getService(element.project, JeElementRegistry::class.java).register(this)
    }

    override fun dispose() {
        disposablePsi.dispose()
    }

    override val psi: T
        get() = disposablePsi()

    val psiManager: PsiManager
        get() = disposablePsi().manager
}

abstract class JeAbstractElement<out T : PsiElement>(element: T) : JeDisposablePsiElementOwner<T>(element), JeElement, Disposable