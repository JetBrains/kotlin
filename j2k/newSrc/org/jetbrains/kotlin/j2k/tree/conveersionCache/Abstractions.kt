/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k.tree.conveersionCache

import org.jetbrains.kotlin.j2k.tree.JKDeclaration
import org.jetbrains.kotlin.j2k.tree.JKElement
import org.jetbrains.kotlin.j2k.tree.visitors.JKTransformer
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor

interface JKMultiverseDeclaration : JKDeclaration{
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <R : JKElement, D> transform(transformer: JKTransformer<D>, data: D): R {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <D> transformChildren(transformer: JKTransformer<D>, data: D) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
