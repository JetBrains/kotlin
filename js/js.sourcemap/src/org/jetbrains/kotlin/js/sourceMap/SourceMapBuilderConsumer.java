/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.sourceMap;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.backend.SourceLocationConsumer;
import org.jetbrains.kotlin.js.backend.ast.JsLocation;
import org.jetbrains.kotlin.js.backend.ast.JsLocationWithSource;
import org.jetbrains.kotlin.resolve.calls.util.CallUtilKt;

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
