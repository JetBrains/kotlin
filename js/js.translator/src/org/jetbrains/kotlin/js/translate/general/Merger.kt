/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.general

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.coroutineMetadata
import org.jetbrains.kotlin.js.inline.clean.resolveTemporaryNames
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

class Merger(private val rootFunction: JsFunction, val internalModuleName: JsName) {
    // Maps unique signature (see generateSignature) to names
    private val nameTable = mutableMapOf<String, JsName>()
    private val importBlock = JsGlobalBlock()
    private val declarationBlock = JsGlobalBlock()
    private val initializerBlock = JsGlobalBlock()
    private val exportBlock = JsGlobalBlock()
    private val declaredImports = mutableSetOf<String>()
    private val classes = mutableMapOf<JsName, JsClassModel>()

    // Add declaration and initialization statements from program fragment to resulting single program
    fun addFragment(fragment: JsProgramFragment) {
        val nameMap = buildNameMap(fragment)
        nameMap.rename(fragment)

        for ((key, importExpr) in fragment.imports) {
            if (declaredImports.add(key)) {
                val name = nameTable[key]!!
                importBlock.statements += JsAstUtils.newVar(nameMap.rename(name), nameMap.rename(importExpr))
            }
        }

        declarationBlock.statements += fragment.declarationBlock
        initializerBlock.statements += fragment.initializerBlock
        exportBlock.statements += fragment.exportBlock
    }

    private fun Map<JsName, JsName>.rename(name: JsName): JsName = getOrElse(name) { name }

    // Builds mapping to map names from different fragments into single name when they denote single declaration
    private fun buildNameMap(fragment: JsProgramFragment): Map<JsName, JsName> {
        val nameMap = mutableMapOf<JsName, JsName>()
        for (nameBinding in fragment.nameBindings) {
            nameMap[nameBinding.name] = nameTable.getOrPut(nameBinding.key) {
                fragment.scope.declareTemporaryName(nameBinding.name.ident).also { it.copyMetadataFrom(nameBinding.name) }
            }
        }
        fragment.scope.findName(Namer.getRootPackageName())?.let { nameMap[it] = internalModuleName }

        return nameMap
    }

    private fun Map<JsName, JsName>.rename(fragment: JsProgramFragment) {
        rename(fragment.declarationBlock)
        rename(fragment.exportBlock)
        rename(fragment.initializerBlock)
    }

    private fun <T: JsNode> Map<JsName, JsName>.rename(node: T): T {
        node.accept(object : RecursiveJsVisitor() {
            override fun visitElement(node: JsNode) {
                super.visitElement(node)
                if (node is HasName) {
                    node.name = node.name?.let { name -> rename(name) }
                }
                if (node is JsFunction) {
                    val coroutineMetadata = node.coroutineMetadata
                    if (coroutineMetadata != null) {
                        node.coroutineMetadata = coroutineMetadata.copy(
                                baseClassRef = rename(coroutineMetadata.baseClassRef),
                                suspendObjectRef = rename(coroutineMetadata.suspendObjectRef)
                        )
                    }
                }
            }
        })
        return node
    }

    // Adds different boilerplate code (like imports, class prototypes, etc) to resulting program.
    fun merge() {
        rootFunction.body.statements.apply {
            this += importBlock.statements
            addClassPrototypes(this)
            this += declarationBlock.statements
            this += initializerBlock.statements
        }
        rootFunction.body.resolveTemporaryNames()
    }

    private fun addClassPrototypes(statements: MutableList<JsStatement>) {
        val visited = mutableSetOf<JsName>()
        for (cls in classes.keys) {
            addClassPrototypes(cls, visited, statements)
        }
    }

    private fun addClassPrototypes(
            name: JsName,
            visited: MutableSet<JsName>,
            statements: MutableList<JsStatement>
    ) {
        if (!visited.add(name)) return
        val cls = classes[name] ?: return
        val superName = cls.superName ?: return

        addClassPrototypes(superName, visited, statements)

        val superclassRef = superName.makeRef()
        val superPrototype = JsAstUtils.prototypeOf(superclassRef)
        val superPrototypeInstance = JsInvocation(JsNameRef("create", "Object"), superPrototype)

        val classRef = name.makeRef()
        val prototype = JsAstUtils.prototypeOf(classRef)
        statements += JsAstUtils.assignment(prototype, superPrototypeInstance).makeStmt()

        val constructorRef = JsNameRef("constructor", prototype.deepCopy())
        statements += JsAstUtils.assignment(constructorRef, classRef.deepCopy()).makeStmt()
    }
}