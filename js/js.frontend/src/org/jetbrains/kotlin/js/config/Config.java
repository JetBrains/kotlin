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

package org.jetbrains.kotlin.js.config;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import kotlin.Function1;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;

import java.util.Collection;
import java.util.List;

/**
 * Base class representing a configuration of translator.
 */
public abstract class Config {
    private final boolean inlineEnabled;

    @NotNull
    private final Project project;
    @Nullable
    private List<JetFile> libFiles = null;
    @NotNull
    private final EcmaVersion target;

    @NotNull
    private final String moduleId;

    private final boolean sourcemap;

    @NotNull
    private final BindingTrace trace = new BindingTraceContext();

    public Config(
            @NotNull Project project,
            @NotNull String moduleId,
            @NotNull EcmaVersion ecmaVersion,
            boolean sourcemap,
            boolean inlineEnabled
    ) {
        this.project = project;
        this.target = ecmaVersion;
        this.moduleId = moduleId;
        this.sourcemap = sourcemap;
        this.inlineEnabled = inlineEnabled;
    }

    public boolean isSourcemap() {
        return sourcemap;
    }

    public boolean isInlineEnabled() {
        return inlineEnabled;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public EcmaVersion getTarget() {
        return target;
    }

    @NotNull
    public String getModuleId() {
        return moduleId;
    }

    public abstract  boolean checkLibFilesAndReportErrors(@NotNull Function1<String, Unit> report);

    @NotNull
    protected abstract List<JetFile> generateLibFiles();

    @NotNull
    public final List<JetFile> getLibFiles() {
        if (libFiles == null) {
            libFiles = generateLibFiles();
        }
        return libFiles;
    }

    @Nullable
    public BindingContext getLibraryContext() {
        return null;
    }

    @NotNull
    public BindingTrace getTrace() {
        return trace;
    }

    @Nullable
    public ModuleDescriptor getLibraryModule() {
        return null;
    }

    @NotNull
    public static Collection<JetFile> withJsLibAdded(@NotNull Collection<JetFile> files, @NotNull Config config) {
        Collection<JetFile> allFiles = Lists.newArrayList();
        allFiles.addAll(files);
        allFiles.addAll(config.getLibFiles());
        return allFiles;
    }

    public boolean isTestConfig() {
        return false;
    }
}
