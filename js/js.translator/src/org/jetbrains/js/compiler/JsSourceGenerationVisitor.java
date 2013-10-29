/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.js.compiler;

import com.google.dart.compiler.backend.js.JsToStringGenerationVisitor;
import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.TextOutput;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.js.compiler.sourcemap.SourceMapBuilder;

import java.util.List;

public class JsSourceGenerationVisitor extends JsToStringGenerationVisitor implements TextOutput.OutListener {
    @Nullable
    private final SourceMapBuilder sourceMapBuilder;

    private final List<Object> pendingSources = new SmartList<Object>();

    public JsSourceGenerationVisitor(TextOutput out, @Nullable SourceMapBuilder sourceMapBuilder) {
        super(out);
        this.sourceMapBuilder = sourceMapBuilder;
        out.setOutListener(this);
    }

    @Override
    public void visitProgramFragment(JsProgramFragment x) {
        x.acceptChildren(this);
    }

    @Override
    public void visitBlock(JsBlock x) {
        printJsBlock(x, false, true);
    }

    @Override
    public void newLined() {
        if (sourceMapBuilder != null) {
            sourceMapBuilder.newLine();
        }
    }

    @Override
    public void indentedAfterNewLine() {
        if (pendingSources.isEmpty()) return;

        assert sourceMapBuilder != null;
        for (Object source : pendingSources) {
            sourceMapBuilder.processSourceInfo(source);
        }
        pendingSources.clear();
    }

    @Override
    public void accept(JsNode node) {
        if (!(node instanceof JsNameRef) && !(node instanceof JsLiteral.JsThisRef)) {
            mapSource(node);
        }
        super.accept(node);
    }

    private void mapSource(JsNode node) {
        if (sourceMapBuilder != null) {
            Object sourceInfo = node.getSource();
            if (sourceInfo != null) {
                if (p.isJustNewlined()) {
                    pendingSources.add(sourceInfo);
                }
                else {
                    sourceMapBuilder.processSourceInfo(sourceInfo);
                }
            }
        }
    }

    @Override
    protected void beforeNodePrinted(JsNode node) {
        mapSource(node);
    }

    @Override
    public void visitProgram(JsProgram program) {
        program.acceptChildren(this);
        if (sourceMapBuilder != null) {
            sourceMapBuilder.addLink();
        }
    }
}
