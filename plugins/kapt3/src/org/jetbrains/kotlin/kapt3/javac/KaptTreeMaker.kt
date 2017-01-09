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

package org.jetbrains.kotlin.kapt3.javac

import com.sun.tools.javac.code.TypeTag
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Names
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Type.*

class KaptTreeMaker(context: Context) : TreeMaker(context) {
    private val nameTable = Names.instance(context).table

    fun Type(type: Type): JCTree.JCExpression {
        convertBuiltinType(type)?.let { return it }
        if (type.sort == ARRAY) {
            return TypeArray(Type(AsmUtil.correctElementType(type)))
        }
        return FqName(type.className)
    }

    fun FqName(internalOrFqName: String): JCTree.JCExpression {
        val path = internalOrFqName.replace('/', '.').split('.')
        assert(path.isNotEmpty())
        if (path.size == 1) return SimpleName(path.single())

        var expr = Select(SimpleName(path[0]), name(path[1]))
        for (index in 2..path.lastIndex) {
            expr = Select(expr, name(path[index]))
        }
        return expr
    }

    fun convertBuiltinType(type: Type): JCTree.JCExpression? {
        val typeTag = when (type) {
            BYTE_TYPE -> TypeTag.BYTE
            BOOLEAN_TYPE -> TypeTag.BOOLEAN
            CHAR_TYPE -> TypeTag.CHAR
            SHORT_TYPE -> TypeTag.SHORT
            INT_TYPE -> TypeTag.INT
            LONG_TYPE -> TypeTag.LONG
            FLOAT_TYPE -> TypeTag.FLOAT
            DOUBLE_TYPE -> TypeTag.DOUBLE
            VOID_TYPE -> TypeTag.VOID
            else -> null
        } ?: return null
        return TypeIdent(typeTag)
    }

    fun SimpleName(name: String): JCTree.JCExpression = Ident(name(name))

    fun name(name: String) = nameTable.fromString(name)

    companion object {
        internal fun preRegister(context: Context) {
            context.put(treeMakerKey, Context.Factory<TreeMaker>(::KaptTreeMaker))
        }
    }
}