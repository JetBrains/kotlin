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
import com.intellij.util.SmartList;
import kotlin.Function1;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil;
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadata;

import java.util.Collection;
import java.util.List;

/**
 * Base class representing a configuration of translator.
 */
public abstract class Config {
    private final boolean inlineEnabled;

    @NotNull
    private final Project project;
    @NotNull
    private final List<JetFile> sourceFilesFromLibraries = new SmartList<JetFile>();
    @NotNull
    private final EcmaVersion target;

    @NotNull
    private final String moduleId;

    private final boolean sourcemap;

    @NotNull
    protected final List<KotlinJavascriptMetadata> metadata = new SmartList<KotlinJavascriptMetadata>();

    @Nullable
    private List<ModuleDescriptorImpl> moduleDescriptors = null;

    private boolean initialized = false;

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

    protected abstract void init(@NotNull List<JetFile> sourceFilesInLibraries, @NotNull List<KotlinJavascriptMetadata> metadata);

    @NotNull
    public List<ModuleDescriptorImpl> getModuleDescriptors() {
        init();
        if (moduleDescriptors != null) return moduleDescriptors;

        moduleDescriptors = new SmartList<ModuleDescriptorImpl>();
        for(KotlinJavascriptMetadata metadataEntry : metadata) {
            moduleDescriptors.add(createModuleDescriptor(metadataEntry));
        }

        return moduleDescriptors;
    }

    @NotNull
    public List<JetFile> getSourceFilesFromLibraries() {
        init();
        return sourceFilesFromLibraries;
    }


    public boolean isTestConfig() {
        return false;
    }

    private void init() {
        if (initialized) return;

        init(sourceFilesFromLibraries, metadata);
        initialized = true;
    }

    private static ModuleDescriptorImpl createModuleDescriptor(KotlinJavascriptMetadata metadata) {
        ModuleDescriptorImpl moduleDescriptor = TopDownAnalyzerFacadeForJS.createJsModule("<" + metadata.getModuleName() + ">");

        List<PackageFragmentProvider> providers = KotlinJavascriptSerializationUtil
                .getPackageFragmentProviders(moduleDescriptor, metadata.getBody());
        CompositePackageFragmentProvider compositePackageFragmentProvider = new CompositePackageFragmentProvider(providers);

        moduleDescriptor.initialize(compositePackageFragmentProvider);
        moduleDescriptor.addDependencyOnModule(moduleDescriptor);
        moduleDescriptor.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
        moduleDescriptor.seal();

        return moduleDescriptor;
    }

    @NotNull
    public static Collection<JetFile> withJsLibAdded(@NotNull Collection<JetFile> files, @NotNull Config config) {
        Collection<JetFile> allFiles = Lists.newArrayList();
        allFiles.addAll(files);
        allFiles.addAll(config.getSourceFilesFromLibraries());
        return allFiles;
    }
}
