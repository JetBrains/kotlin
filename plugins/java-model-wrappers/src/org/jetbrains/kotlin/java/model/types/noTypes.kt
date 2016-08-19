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

import javax.lang.model.type.*

interface JeNoType : JeTypeMirror, NoType

object JePackageTypeMirror : JeNoType {
    override fun getKind() = TypeKind.PACKAGE
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitNoType(this, p)
    override fun toString() = "<package>"
}

object JeNoneType : JeNoType {
    override fun getKind() = TypeKind.NONE
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitNoType(this, p)
    override fun toString() = "<none>"
}

object JeVoidType : JeNoType {
    override fun getKind() = TypeKind.VOID
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitNoType(this, p)
    override fun toString() = "void"
}

object JeErrorType : JeNoType {
    override fun getKind() = TypeKind.ERROR
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitNoType(this, p)
    override fun toString() = "<error>"
}

object JeDeclaredErrorType : JeNoType, DeclaredType {
    override fun getKind() = TypeKind.ERROR
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitNoType(this, p)

    override fun getTypeArguments() = emptyList<TypeMirror>()
    override fun asElement() = null
    override fun getEnclosingType() = JeNoneType
}