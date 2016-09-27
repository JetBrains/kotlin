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

package org.jetbrains.kotlin.java.model.internal

import com.intellij.openapi.Disposable

/*
    Some annotation processors (I'm looking at you, Data Binding)
      have a habit to cache Element or TypeMirror instances into static fields.
    Because almost every JeElement or JeTypeMirror is dependent on some PsiElement, and PsiElements are dependent on PsiProject,
      the entire project will not be gc'ed normally.
    JeElementRegistry allows us to remove all Psi*-references from our wrapper Element and TypeMirror instances.

    So this class basically remembers all instances created by kapt, and disposes all of them as the Annotation Processing is complete.
 */
class JeElementRegistry : Disposable {
    private val list = mutableListOf<Disposable>()

    fun register(obj: Disposable) {
        list += obj
    }

    override fun dispose() {
        val list = this.list
        list.forEach { it.dispose() }
        this.list.clear()
    }
}