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

package org.jetbrains.kotlin.serialization.js.ast

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.JsImportedModule
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.serialization.js.ast.JsAstProtoBuf.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader

class JsAstDeserializer(program: JsProgram, private val sourceRoots: Iterable<File>) : JsAstDeserializerBase() {

    override val scope: JsRootScope = JsRootScope(program)

    fun deserialize(input: InputStream): JsProgramFragment {
        return deserialize(Chunk.parseFrom(CodedInputStream.newInstance(input).apply { setRecursionLimit(4096) }))
    }

    fun deserialize(proto: Chunk): JsProgramFragment {
        stringTable += proto.stringTable.entryList
        nameTable += proto.nameTable.entryList
        nameCache += nameTable.map { null }
        try {
            return deserialize(proto.fragment)
        } finally {
            stringTable.clear()
            nameTable.clear()
            nameCache.clear()
        }
    }

    private fun deserialize(proto: Fragment): JsProgramFragment {
        val fragment = JsProgramFragment(scope, proto.packageFqn)

        fragment.importedModules += proto.importedModuleList.map { importedModuleProto ->
            JsImportedModule(
                deserializeString(importedModuleProto.externalNameId),
                deserializeName(importedModuleProto.internalNameId),
                if (importedModuleProto.hasPlainReference()) deserialize(importedModuleProto.plainReference) else null
            )
        }

        fragment.imports += proto.importEntryList.associate { importProto ->
            deserializeString(importProto.signatureId) to deserialize(importProto.expression)
        }

        if (proto.hasDeclarationBlock()) {
            fragment.declarationBlock.statements += deserializeCompositeBlock(proto.declarationBlock).statements
        }
        if (proto.hasInitializerBlock()) {
            fragment.initializerBlock.statements += deserializeCompositeBlock(proto.initializerBlock).statements
        }
        if (proto.hasExportBlock()) {
            fragment.exportBlock.statements += deserializeCompositeBlock(proto.exportBlock).statements
        }

        fragment.nameBindings += proto.nameBindingList.map { nameBindingProto ->
            JsNameBinding(deserializeString(nameBindingProto.signatureId), deserializeName(nameBindingProto.nameId))
        }

        fragment.classes += proto.classModelList.associate { clsProto -> deserialize(clsProto).let { it.name to it } }

        val moduleExpressions = proto.moduleExpressionList.map { deserialize(it) }
        fragment.inlineModuleMap += proto.inlineModuleList.associate { inlineModuleProto ->
            deserializeString(inlineModuleProto.signatureId) to moduleExpressions[inlineModuleProto.expressionId]
        }

        for (nameBinding in fragment.nameBindings) {
            if (nameBinding.key in fragment.imports) {
                nameBinding.name.imported = true
            }
        }

        if (proto.hasTestsInvocation()) {
            fragment.tests = deserialize(proto.testsInvocation)
        }

        if (proto.hasMainInvocation()) {
            fragment.mainFunction = deserialize(proto.mainInvocation)
        }

        proto.inlinedLocalDeclarationsList.forEach {
            fragment.inlinedLocalDeclarations[deserializeString(it.tag)] = deserializeCompositeBlock(it.block)
        }

        return fragment
    }

    private fun deserialize(proto: ClassModel): JsClassModel {
        val superName = if (proto.hasSuperNameId()) deserializeName(proto.superNameId) else null
        return JsClassModel(deserializeName(proto.nameId), superName).apply {
            proto.interfaceNameIdList.mapTo(interfaces) { deserializeName(it) }
            if (proto.hasPostDeclarationBlock()) {
                postDeclarationBlock.statements += deserializeCompositeBlock(proto.postDeclarationBlock).statements
            }
        }
    }

    override fun embedSources(deserializedLocation: JsLocation, file: String): JsLocationWithSource? {
        val contentFile = sourceRoots
            .map { File(it, file) }
            .firstOrNull { it.exists() }

        return if (contentFile != null) {
            JsLocationWithEmbeddedSource(deserializedLocation, null) { InputStreamReader(FileInputStream(contentFile), "UTF-8") }
        } else null
    }
}
