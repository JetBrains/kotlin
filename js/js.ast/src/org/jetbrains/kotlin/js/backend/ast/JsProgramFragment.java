// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class JsProgramFragment {
    private final JsScope scope;
    private final List<JsImportedModule> importedModules = new ArrayList<>();
    private final Map<String, JsExpression> imports = new LinkedHashMap<>();
    private final JsGlobalBlock declarationBlock = new JsGlobalBlock();
    private final JsGlobalBlock exportBlock = new JsGlobalBlock();
    private final JsGlobalBlock initializerBlock = new JsGlobalBlock();
    private final List<JsNameBinding> nameBindings = new ArrayList<>();
    private final Map<JsName, JsClassModel> classes = new LinkedHashMap<>();
    private final Map<String, JsExpression> inlineModuleMap = new LinkedHashMap<>();

    public JsProgramFragment(@NotNull JsScope scope) {
        this.scope = scope;
    }

    @NotNull
    public JsScope getScope() {
        return scope;
    }

    @NotNull
    public List<JsImportedModule> getImportedModules() {
        return importedModules;
    }

    @NotNull
    public Map<String, JsExpression> getImports() {
        return imports;
    }

    @NotNull
    public JsGlobalBlock getDeclarationBlock() {
        return declarationBlock;
    }

    @NotNull
    public JsGlobalBlock getExportBlock() {
        return exportBlock;
    }

    @NotNull
    public JsGlobalBlock getInitializerBlock() {
        return initializerBlock;
    }

    @NotNull
    public List<JsNameBinding> getNameBindings() {
        return nameBindings;
    }

    @NotNull
    public Map<JsName, JsClassModel> getClasses() {
        return classes;
    }

    @NotNull
    public Map<String, JsExpression> getInlineModuleMap() {
        return inlineModuleMap;
    }
}
