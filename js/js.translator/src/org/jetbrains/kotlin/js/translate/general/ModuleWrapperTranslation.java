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

package org.jetbrains.kotlin.js.translate.general;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.serialization.js.ModuleKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleWrapperTranslation {
    private ModuleWrapperTranslation() {
    }

    @NotNull
    public static List<JsStatement> wrapIfNecessary(
            @Nullable String moduleId,
            @NotNull JsExpression function,
            @NotNull List<ImportedModule> importedModules,
            @NotNull JsProgram program,
            @NotNull ModuleKind kind
    ) {
        switch (kind) {
            case AMD:
                return wrapAmd(moduleId, function, importedModules, program);
            case COMMON_JS:
                return wrapCommonJs(function, importedModules, program);
            case UMD:
                return wrapUmd(moduleId, function, importedModules, program);
            case PLAIN:
            default:
                return wrapPlain(moduleId, function, importedModules, program);
        }
    }

    @NotNull
    private static List<JsStatement> wrapUmd(
            @Nullable String moduleId,
            @NotNull JsExpression function,
            @NotNull List<ImportedModule> importedModules,
            @NotNull JsProgram program
    ) {
        JsScope scope = program.getScope();
        JsName rootName = scope.declareName("root");
        JsName factoryName = scope.declareName("factory");
        JsName defineName = scope.declareName("define");
        JsName exportsName = scope.declareName("exports");

        JsExpression amdTest = JsAstUtils.and(JsAstUtils.typeOfIs(defineName.makeRef(), program.getStringLiteral("function")),
                                              new JsNameRef("amd", defineName.makeRef()));
        JsExpression commonJsTest = JsAstUtils.typeOfIs(exportsName.makeRef(), program.getStringLiteral("object"));

        JsBlock amdBody = new JsBlock(wrapAmd(moduleId, factoryName.makeRef(), importedModules, program));
        JsBlock commonJsBody = new JsBlock(wrapCommonJs(factoryName.makeRef(), importedModules, program));
        JsInvocation plainInvocation = makePlainInvocation(factoryName.makeRef(), importedModules, program);

        JsExpression plainExpr;
        if (moduleId != null) {
            JsExpression lhs = Namer.requiresEscaping(moduleId) ?
                               new JsArrayAccess(rootName.makeRef(), program.getStringLiteral(moduleId)) :
                               new JsNameRef(scope.declareName(moduleId), rootName.makeRef());
            plainExpr = JsAstUtils.assignment(lhs, plainInvocation);
        }
        else {
            plainExpr = plainInvocation;
        }

        JsStatement selector = JsAstUtils.newJsIf(amdTest, amdBody, JsAstUtils.newJsIf(commonJsTest, commonJsBody, plainExpr.makeStmt()));
        JsFunction adapter = new JsFunction(program.getScope(), new JsBlock(selector), "UMD adapter");
        adapter.getParameters().add(new JsParameter(rootName));
        adapter.getParameters().add(new JsParameter(factoryName));

        return Collections.singletonList(new JsInvocation(adapter, JsLiteral.THIS, function).makeStmt());
    }

    @NotNull
    private static List<JsStatement> wrapAmd(
            @Nullable String moduleId,
            @NotNull JsExpression function,
            @NotNull List<ImportedModule> importedModules,
            @NotNull JsProgram program
    ) {
        JsScope scope = program.getScope();
        JsName defineName = scope.declareName("define");
        List<JsExpression> invocationArgs = new ArrayList<JsExpression>();

        if (moduleId != null) {
            invocationArgs.add(program.getStringLiteral(moduleId));
        }

        List<JsExpression> moduleNameList = new ArrayList<JsExpression>(importedModules.size());
        for (ImportedModule importedModule : importedModules) {
            moduleNameList.add(program.getStringLiteral(importedModule.id));
        }
        invocationArgs.add(new JsArrayLiteral(moduleNameList));

        invocationArgs.add(function);

        JsInvocation invocation = new JsInvocation(defineName.makeRef(), invocationArgs);
        return Collections.singletonList(invocation.makeStmt());
    }

    @NotNull
    private static List<JsStatement> wrapCommonJs(
            @NotNull JsExpression function,
            @NotNull List<ImportedModule> importedModules,
            @NotNull JsProgram program
    ) {
        JsScope scope = program.getScope();
        JsName moduleName = scope.declareName("module");
        JsName requireName = scope.declareName("require");

        List<JsExpression> invocationArgs = new ArrayList<JsExpression>();
        for (ImportedModule importedModule : importedModules) {
            invocationArgs.add(new JsInvocation(requireName.makeRef(), program.getStringLiteral(importedModule.id)));
        }

        JsInvocation invocation = new JsInvocation(function, invocationArgs);
        JsExpression assignment = JsAstUtils.assignment(new JsNameRef("exports", moduleName.makeRef()), invocation);
        return Collections.singletonList(assignment.makeStmt());
    }

    @NotNull
    private static List<JsStatement> wrapPlain(
            @Nullable String moduleId,
            @NotNull JsExpression function,
            @NotNull List<ImportedModule> importedModules,
            @NotNull JsProgram program
    ) {
        JsInvocation invocation = makePlainInvocation(function, importedModules, program);

        JsStatement statement;
        if (moduleId == null) {
            statement = invocation.makeStmt();
        }
        else {
            statement = Namer.requiresEscaping(moduleId) ?
                        JsAstUtils.assignment(makePlainModuleRef(moduleId, program), invocation).makeStmt() :
                        JsAstUtils.newVar(program.getRootScope().declareName(moduleId), invocation);
        }

        return Collections.singletonList(statement);
    }

    @NotNull
    private static JsInvocation makePlainInvocation(@NotNull JsExpression function, @NotNull List<ImportedModule> importedModules,
            @NotNull JsProgram program) {
        List<JsExpression> invocationArgs = new ArrayList<JsExpression>(importedModules.size());

        for (ImportedModule importedModule : importedModules) {
            invocationArgs.add(makePlainModuleRef(importedModule.id, program));
        }

        return new JsInvocation(function, invocationArgs);
    }

    @NotNull
    private static JsExpression makePlainModuleRef(@NotNull String moduleId, @NotNull JsProgram program) {
        // TODO: we could use `this.moduleName` syntax. However, this does not work for `kotlin` module in Rhino, since
        // we run kotlin.js in a parent scope. Consider better solution
        return Namer.requiresEscaping(moduleId) ?
               new JsArrayAccess(JsLiteral.THIS, program.getStringLiteral(moduleId)) :
               program.getScope().declareName(moduleId).makeRef();
    }

    static final class ImportedModule {
        @NotNull public final String id;
        @NotNull public final JsName name;

        public ImportedModule(@NotNull String id, @NotNull JsName name) {
            this.id = id;
            this.name = name;
        }
    }
}
