/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.facade;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.backend.SourceLocationConsumer;
import org.jetbrains.kotlin.js.backend.ast.JsLocation;
import org.jetbrains.kotlin.js.backend.ast.JsLocationWithSource;
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver;
import org.jetbrains.kotlin.js.sourceMap.SourceMapMappingConsumer;
import org.jetbrains.kotlin.js.translate.utils.PsiUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SourceMapBuilderConsumer implements SourceLocationConsumer {
    @NotNull
    private final File sourceBaseDir;

    @NotNull
    private final SourceMapMappingConsumer mappingConsumer;

    @NotNull
    private final SourceFilePathResolver pathResolver;

    private final boolean provideCurrentModuleContent;

    private final boolean provideExternalModuleContent;

    @NotNull
    private final List<Object> sourceStack = new ArrayList<>();

    public SourceMapBuilderConsumer(
            @NotNull File sourceBaseDir,
            @NotNull SourceMapMappingConsumer mappingConsumer,
            @NotNull SourceFilePathResolver pathResolver,
            boolean provideCurrentModuleContent, boolean provideExternalModuleContent
    ) {
        this.sourceBaseDir = sourceBaseDir;
        this.mappingConsumer = mappingConsumer;
        this.pathResolver = pathResolver;
        this.provideCurrentModuleContent = provideCurrentModuleContent;
        this.provideExternalModuleContent = provideExternalModuleContent;
    }

    @Override
    public void newLine() {
        mappingConsumer.newLine();
    }

    @Override
    public void pushSourceInfo(@Nullable Object info) {
        sourceStack.add(info);
        addMapping(info);
    }

    @Override
    public void popSourceInfo() {
        sourceStack.remove(sourceStack.size() - 1);
        Object sourceInfo = !sourceStack.isEmpty() ? sourceStack.get(sourceStack.size() - 1) : null;
        addMapping(sourceInfo);
    }

    private void addMapping(@Nullable Object sourceInfo) {
        if (sourceInfo == null) {
            mappingConsumer.addEmptyMapping();
        }
        if (sourceInfo instanceof PsiElement) {
            PsiElement element = (PsiElement) sourceInfo;
            if (CallUtilKt.isFakePsiElement(element)) return;
            try {
                JsLocation location = PsiUtils.extractLocationFromPsi(element, pathResolver);
                PsiFile psiFile = element.getContainingFile();
                File file = new File(psiFile.getViewProvider().getVirtualFile().getPath());
                Supplier<Reader> contentSupplier;
                if (provideCurrentModuleContent) {
                    contentSupplier = () -> {
                        try {
                            return new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8"));
                        }
                        catch (IOException e) {
                            return null;
                        }
                    };
                }
                else {
                    contentSupplier = () -> null;
                }
                mappingConsumer.addMapping(location.getFile(), null, contentSupplier, location.getStartLine(), location.getStartChar());
            }
            catch (IOException e) {
                throw new RuntimeException("IO error occurred generating source maps", e);
            }
        }
        else if (sourceInfo instanceof JsLocationWithSource) {
            JsLocationWithSource location = (JsLocationWithSource) sourceInfo;
            Supplier<Reader> contentSupplier = provideExternalModuleContent ? location.getSourceProvider()::invoke : () -> null;
            String path;

            File absFile = new File(location.getFile()).isAbsolute() ?
                           new File(location.getFile()) :
                           new File(sourceBaseDir, location.getFile());
            if (absFile.isAbsolute()) {
                try {
                    path = pathResolver.getPathRelativeToSourceRoots(absFile);
                }
                catch (IOException e) {
                    path = location.getFile();
                }
            }
            else {
                path = location.getFile();
            }
            mappingConsumer.addMapping(path, location.getIdentityObject(), contentSupplier,
                               location.getStartLine(), location.getStartChar());
        }
    }
}
