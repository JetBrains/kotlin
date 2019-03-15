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

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.exportedPackage
import org.jetbrains.kotlin.js.backend.ast.metadata.exportedTag
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.createPrototypeStatements
import org.jetbrains.kotlin.js.translate.utils.definePackageAlias
import org.jetbrains.kotlin.serialization.js.ModuleKind

class Merger(
    val module: ModuleDescriptor,
    val moduleId: String,
    val moduleKind: ModuleKind
) {
    val program: JsProgram = JsProgram()

    val internalModuleName = program.scope.declareName("_")

    private val rootFunction = JsFunction(program.rootScope, JsBlock(), "root function").also {
        it.parameters.add(JsParameter(internalModuleName))
    }

    // Maps unique signature (see generateSignature) to names
    private val nameTable = mutableMapOf<String, JsName>()

    private val importedModuleTable = mutableMapOf<JsImportedModuleKey, JsName>()
    private val importBlock = JsGlobalBlock()
    private val declarationBlock = JsGlobalBlock()
    private val initializerBlock = JsGlobalBlock()
    private val testsMap = mutableMapOf<String, JsStatement>()
    private var mainFn: Pair<String, JsStatement>? = null
    private val exportBlock = JsGlobalBlock()
    private val declaredImports = mutableSetOf<String>()
    private val classes = mutableMapOf<JsName, JsClassModel>()
    private val importedModulesImpl = mutableListOf<JsImportedModule>()
    private val exportedPackages = mutableMapOf<String, JsName>()
    private val exportedTags = mutableSetOf<String>()

    private val fragments = mutableListOf<JsProgramFragment>()

    // Add declaration and initialization statements from program fragment to resulting single program
    fun addFragment(fragment: JsProgramFragment) {
        fragments.add(fragment)
    }

    private fun JsProgramFragment.tryUpdateTests() {
        tests?.let { newTests ->
            testsMap.computeIfAbsent(packageFqn) {
                // Copying is needed to prevent adding tests from another fragment into this one.
                // The statements have to be original so that they are affected by the optimizations
                // This is a temporary workaround which becomes obsolete when program construction is postponed until after IC serialization.
                newTests.deepCopy().also {
                    it.decomposeTestInvocation()?.statements?.clear()
                }
            }.let { oldTests ->
                oldTests.decomposeTestInvocation()?.let { oldTestBody ->
                    newTests.decomposeTestInvocation()?.let { newTestBody ->
                        oldTestBody.statements += newTestBody.statements
                    }
                }
            }
        }
    }

    private fun JsStatement.decomposeTestInvocation(): JsBlock? {
        return (this as? JsExpressionStatement)?.let {
            (it.expression as? JsInvocation)?.let {
                (it.arguments.getOrNull(2) as? JsFunction)?.body
            }
        }
    }

    private fun JsProgramFragment.tryUpdateMain() {
        mainFunction?.let { m ->
            val currentMainFqn = mainFn?.first
            if (currentMainFqn == null || currentMainFqn > packageFqn) {
                mainFn = packageFqn to m
            }
        }
    }

    val importedModules: List<JsImportedModule>
        get() = importedModulesImpl

    private fun Map<JsName, JsName>.rename(name: JsName): JsName = getOrElse(name) { name }

    // Builds mapping to map names from different fragments into single name when they denote single declaration
    private fun buildNameMap(fragment: JsProgramFragment): Map<JsName, JsName> {
        val nameMap = mutableMapOf<JsName, JsName>()
        for (nameBinding in fragment.nameBindings) {
            nameMap[nameBinding.name] = nameTable.getOrPut(nameBinding.key) {
                JsScope.declareTemporaryName(nameBinding.name.ident).also { it.copyMetadataFrom(nameBinding.name) }
            }
        }
        fragment.scope.findName(Namer.getRootPackageName())?.let { nameMap[it] = internalModuleName }

        for (importedModule in fragment.importedModules) {
            nameMap[importedModule.internalName] = importedModuleTable.getOrPut(importedModule.key) {
                val scope = rootFunction.scope
                val newName = importedModule.internalName.let {
                    if (it.isTemporary) JsScope.declareTemporaryName(it.ident) else scope.declareName(it.ident)
                }
                newName.also {
                    importedModulesImpl += JsImportedModule(importedModule.externalName, it, importedModule.plainReference)
                    it.copyMetadataFrom(importedModule.internalName)
                }
            }
        }

        return nameMap
    }

    private fun addExportStatements(fragment: JsProgramFragment) {
        val nameMap = mutableMapOf<JsName, JsName>()
        for (statement in fragment.exportBlock.statements) {
            if (statement is JsVars && statement.exportedPackage != null) {
                val exportedPackage = statement.exportedPackage!!
                val localName = statement.vars[0].name
                if (exportedPackage in exportedPackages) {
                    nameMap[localName] = exportedPackages[exportedPackage]!!
                    continue
                } else {
                    exportedPackages[exportedPackage] = localName
                }
            } else if (statement is JsExpressionStatement) {
                val exportedTag = statement.exportedTag
                if (exportedTag != null && !exportedTags.add(exportedTag)) continue
            }
            exportBlock.statements += nameMap.rename(statement.deepCopy())
        }
    }

    private fun Map<JsName, JsName>.rename(fragment: JsProgramFragment) {
        rename(fragment.declarationBlock)
        rename(fragment.exportBlock)
        rename(fragment.initializerBlock)

        fragment.nameBindings.forEach { it.name = rename(it.name) }
        fragment.imports.entries.forEach { it.setValue(rename(it.value)) }

        fragment.importedModules.forEach { import ->
            import.internalName = rename(import.internalName)
            import.plainReference?.let { rename(it) }
        }

        val classes = fragment.classes.values.map { cls ->
            JsClassModel(rename(cls.name), cls.superName?.let { rename(it) }).apply {
                postDeclarationBlock.statements += rename(cls.postDeclarationBlock).statements
                cls.interfaces.mapTo(interfaces) { rename(it) }
            }
        }
        fragment.classes.clear()
        fragment.classes += classes.map { it.name to it }

        fragment.inlineModuleMap.forEach { (_, value) -> rename(value) }

        fragment.tests?.let { rename(it) }
        fragment.mainFunction?.let { rename(it) }
        fragment.inlinedLocalDeclarations.values.forEach { rename(it) }
    }

    private fun <T : JsNode> Map<JsName, JsName>.rename(rootNode: T): T {
        rootNode.accept(object : RecursiveJsVisitor() {
            override fun visitElement(node: JsNode) {
                super.visitElement(node)
                if (node is HasName) {
                    node.name = node.name?.let { rename(it) }
                }
            }
        })
        return rootNode
    }

    private val importedFunctionWrappers = mutableMapOf<String, JsGlobalBlock>()

    private fun mergeNames() {
        for (fragment in fragments) {
            val nameMap = buildNameMap(fragment)
            nameMap.rename(fragment)

            for ((key, importExpr) in fragment.imports) {
                if (declaredImports.add(key)) {
                    val name = nameTable[key]!!
                    importBlock.statements += JsAstUtils.newVar(name, importExpr)
                }
            }

            declarationBlock.statements += fragment.declarationBlock
            initializerBlock.statements += fragment.initializerBlock
            fragment.tryUpdateTests()
            fragment.tryUpdateMain()
            addExportStatements(fragment)

            fragment.inlinedLocalDeclarations.forEach { (tag, imports) ->
                importedFunctionWrappers.putIfAbsent(tag, imports)
            }

            classes += fragment.classes
        }
    }

    // TODO consider using metadata to determine current-module fake override statements
    // The approach could be similar to JsName.alias which is used to avoid re-importing current module declarations
    private fun JsStatement?.isFakeOverrideAssignment(): Boolean {
        fun JsExpression?.isMemberReference(): Boolean {
            val qualifier = (this as? JsNameRef)?.qualifier as? JsNameRef ?: return false

            return qualifier.name == null && qualifier.ident == "prototype"
        }

        val binOp = (this as? JsExpressionStatement)?.expression as? JsBinaryOperation ?: return false

        return binOp.operator == JsBinaryOperator.ASG && binOp.arg1.isMemberReference() && binOp.arg2.isMemberReference()
    }

    // Adds different boilerplate code (like imports, class prototypes, etc) to resulting program.
    fun merge() {
        mergeNames()

        rootFunction.body.statements.apply {
            addImportForInlineDeclarationIfNecessary()
            this += importBlock.statements
            addClassPrototypes(this)

            // TODO better placing?
            val additionalFakeOverrides = mutableListOf<JsStatement>()
            importedFunctionWrappers.values.forEach { block ->
                block.statements.forEach { s ->
                    if (s.isFakeOverrideAssignment()) {
                        additionalFakeOverrides += s
                    } else {
                        this += s
                    }
                }
            }

            this += declarationBlock.statements
            this += exportBlock.statements
            addClassPostDeclarations(this)
            this += additionalFakeOverrides
            this += initializerBlock.statements
            this += testsMap.values
            mainFn?.second?.let { this += it }
        }
    }

    fun buildProgram(): JsProgram {
        merge()

        val rootBlock = rootFunction.getBody()

        val statements = rootBlock.getStatements()

        statements.add(0, JsStringLiteral("use strict").makeStmt())
        if (!isBuiltinModule(fragments)) {
            defineModule(program, statements, moduleId)
        }

        // Invoke function passing modules as arguments
        // This should help minifier tool to recognize references to these modules as local variables and make them shorter.
        for (importedModule in importedModules) {
            rootFunction.parameters.add(JsParameter(importedModule.internalName))
        }

        statements.add(JsReturn(internalModuleName.makeRef()))

        val block = program.globalBlock
        block.statements.addAll(
            ModuleWrapperTranslation.wrapIfNecessary(
                moduleId, rootFunction, importedModules, program,
                moduleKind
            )
        )

        return program
    }

    private fun MutableList<JsStatement>.addImportForInlineDeclarationIfNecessary() {
        val importsForInlineName = nameTable[Namer.IMPORTS_FOR_INLINE_PROPERTY] ?: return
        this += definePackageAlias(
            Namer.IMPORTS_FOR_INLINE_PROPERTY, importsForInlineName, Namer.IMPORTS_FOR_INLINE_PROPERTY,
            JsNameRef(Namer.getRootPackageName())
        )
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

        statements += createPrototypeStatements(superName, name)
    }

    private fun addClassPostDeclarations(statements: MutableList<JsStatement>) {
        val visited = mutableSetOf<JsName>()
        for (cls in classes.keys) {
            addClassPostDeclarations(cls, visited, statements)
        }
    }

    private fun addClassPostDeclarations(
        name: JsName,
        visited: MutableSet<JsName>,
        statements: MutableList<JsStatement>
    ) {
        if (!visited.add(name)) return
        val cls = classes[name] ?: return
        cls.superName?.let { addClassPostDeclarations(it, visited, statements) }
        cls.interfaces.forEach { addClassPostDeclarations(it, visited, statements) }
        statements += cls.postDeclarationBlock.statements
    }

    companion object {
        private val ENUM_SIGNATURE = "kotlin\$Enum"

        // TODO is there no better way?
        private fun isBuiltinModule(fragments: List<JsProgramFragment>): Boolean {
            for (fragment in fragments) {
                for (nameBinding in fragment.nameBindings) {
                    if (nameBinding.key == ENUM_SIGNATURE && !fragment.imports.containsKey(ENUM_SIGNATURE)) {
                        return true
                    }
                }
            }
            return false
        }

        private fun defineModule(program: JsProgram, statements: MutableList<JsStatement>, moduleId: String) {
            val rootPackageName = program.scope.findName(Namer.getRootPackageName())
            if (rootPackageName != null) {
                val namer = Namer.newInstance(program.scope)
                statements.add(
                    JsInvocation(
                        namer.kotlin("defineModule"),
                        JsStringLiteral(moduleId),
                        rootPackageName.makeRef()
                    ).makeStmt()
                )
            }
        }
    }
}
