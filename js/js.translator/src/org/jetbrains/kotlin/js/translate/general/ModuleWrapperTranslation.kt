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

package org.jetbrains.kotlin.js.translate.general

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.serialization.js.ModuleKind

object ModuleWrapperTranslation {
    @JvmStatic fun wrapIfNecessary(
            moduleId: String, function: JsExpression, importedModules: List<JsImportedModule>,
            program: JsProgram, kind: ModuleKind
    ): List<JsStatement> {
        return when (kind) {
            ModuleKind.AMD -> wrapAmd(function, importedModules, program)
            ModuleKind.COMMON_JS -> wrapCommonJs(function, importedModules, program)
            ModuleKind.UMD -> wrapUmd(moduleId, function, importedModules, program)
            ModuleKind.PLAIN -> wrapPlain(moduleId, function, importedModules, program)
        }
    }

    private fun wrapUmd(
            moduleId: String, function: JsExpression,
            importedModules: List<JsImportedModule>, program: JsProgram
    ): List<JsStatement> {
        val scope = program.scope
        val defineName = scope.declareName("define")
        val exportsName = scope.declareName("exports")

        val adapterBody = JsBlock()
        val adapter = JsFunction(program.scope, adapterBody, "Adapter")
        val rootName = adapter.scope.declareName("root")
        val factoryName = adapter.scope.declareName("factory")
        adapter.parameters += JsParameter(rootName)
        adapter.parameters += JsParameter(factoryName)

        val amdTest = JsAstUtils.and(JsAstUtils.typeOfIs(defineName.makeRef(), program.getStringLiteral("function")),
                                     JsNameRef("amd", defineName.makeRef()))
        val commonJsTest = JsAstUtils.typeOfIs(exportsName.makeRef(), program.getStringLiteral("object"))

        val amdBody = JsBlock(wrapAmd(factoryName.makeRef(), importedModules, program))
        val commonJsBody = JsBlock(wrapCommonJs(factoryName.makeRef(), importedModules, program))
        val plainInvocation = makePlainInvocation(moduleId, factoryName.makeRef(), importedModules, program)

        val lhs: JsExpression = if (Namer.requiresEscaping(moduleId)) {
            JsArrayAccess(rootName.makeRef(), program.getStringLiteral(moduleId))
        }
        else {
            JsNameRef(scope.declareName(moduleId), rootName.makeRef())
        }

        val plainBlock = JsBlock()
        for (importedModule in importedModules) {
            plainBlock.statements += addModuleValidation(moduleId, program, importedModule)
        }
        plainBlock.statements += JsAstUtils.assignment(lhs, plainInvocation).makeStmt()

        val selector = JsAstUtils.newJsIf(amdTest, amdBody, JsAstUtils.newJsIf(commonJsTest, commonJsBody, plainBlock))
        adapterBody.statements += selector

        return listOf(JsInvocation(adapter, JsLiteral.THIS, function).makeStmt())
    }

    private fun wrapAmd(
            function: JsExpression,
            importedModules: List<JsImportedModule>, program: JsProgram
    ): List<JsStatement> {
        val scope = program.scope
        val defineName = scope.declareName("define")
        val invocationArgs = listOf(
                JsArrayLiteral(listOf(program.getStringLiteral("exports")) + importedModules.map { program.getStringLiteral(it.externalName) }),
                function
        )

        val invocation = JsInvocation(defineName.makeRef(), invocationArgs)
        return listOf(invocation.makeStmt())
    }

    private fun wrapCommonJs(
            function: JsExpression,
            importedModules: List<JsImportedModule>,
            program: JsProgram
    ): List<JsStatement> {
        val scope = program.scope
        val moduleName = scope.declareName("module")
        val requireName = scope.declareName("require")

        val invocationArgs = importedModules.map { JsInvocation(requireName.makeRef(), program.getStringLiteral(it.externalName)) }
        val invocation = JsInvocation(function, listOf(JsNameRef("exports", moduleName.makeRef())) + invocationArgs)
        return listOf(invocation.makeStmt())
    }

    private fun wrapPlain(
            moduleId: String, function: JsExpression,
            importedModules: List<JsImportedModule>, program: JsProgram
    ): List<JsStatement> {
        val invocation = makePlainInvocation(moduleId, function, importedModules, program)
        val statements = mutableListOf<JsStatement>()

        for (importedModule in importedModules) {
            statements += addModuleValidation(moduleId, program, importedModule)
        }

        statements += if (Namer.requiresEscaping(moduleId)) {
            JsAstUtils.assignment(makePlainModuleRef(moduleId, program), invocation).makeStmt()
        }
        else {
            JsAstUtils.newVar(program.rootScope.declareName(moduleId), invocation)
        }

        return statements
    }

    private fun addModuleValidation(
            currentModuleId: String,
            program: JsProgram,
            module: JsImportedModule
    ): JsStatement {
        val moduleRef = makePlainModuleRef(module, program)
        val moduleExistsCond = JsAstUtils.typeOfIs(moduleRef, program.getStringLiteral("undefined"))
        val moduleNotFoundMessage = program.getStringLiteral(
                "Error loading module '" + currentModuleId + "'. Its dependency '" + module.externalName + "' was not found. " +
                "Please, check whether '" + module.externalName + "' is loaded prior to '" + currentModuleId + "'.")
        val moduleNotFoundThrow = JsThrow(JsNew(JsNameRef("Error"), listOf<JsExpression>(moduleNotFoundMessage)))
        return JsIf(moduleExistsCond, JsBlock(moduleNotFoundThrow))
    }

    private fun makePlainInvocation(
            moduleId: String,
            function: JsExpression,
            importedModules: List<JsImportedModule>,
            program: JsProgram
    ): JsInvocation {
        val invocationArgs = importedModules.map { makePlainModuleRef(it, program) }
        val moduleRef = makePlainModuleRef(moduleId, program)
        val testModuleDefined = JsAstUtils.typeOfIs(moduleRef, program.getStringLiteral("undefined"))
        val selfArg = JsConditional(testModuleDefined, JsObjectLiteral(false), moduleRef.deepCopy())

        return JsInvocation(function, listOf(selfArg) + invocationArgs)
    }

    private fun makePlainModuleRef(module: JsImportedModule, program: JsProgram): JsExpression {
        return module.plainReference ?: makePlainModuleRef(module.externalName, program)
    }

    private fun makePlainModuleRef(moduleId: String, program: JsProgram): JsExpression {
        // TODO: we could use `this.moduleName` syntax. However, this does not work for `kotlin` module in Rhino, since
        // we run kotlin.js in a parent scope. Consider better solution
        return if (Namer.requiresEscaping(moduleId)) {
            JsArrayAccess(JsLiteral.THIS, program.getStringLiteral(moduleId))
        }
        else {
            program.scope.declareName(moduleId).makeRef()
        }
    }
}
