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
import kotlin.CollectionsKt;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.js.resolve.JsPlatform;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.TargetPlatformKt;
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadata;
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils;

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
    private final LockBasedStorageManager storageManager = new LockBasedStorageManager();
    @NotNull
    private final List<KtFile> sourceFilesFromLibraries = new SmartList<KtFile>();
    @NotNull
    private final EcmaVersion target;

    @NotNull
    private final String moduleId;

    private final boolean sourcemap;
    private final boolean metaInfo;

    @NotNull
    protected final List<KotlinJavascriptMetadata> metadata = new SmartList<KotlinJavascriptMetadata>();

    @Nullable
    private List<ModuleDescriptorImpl> moduleDescriptors = null;

    private boolean initialized = false;

    protected Config(
            @NotNull Project project,
            @NotNull String moduleId,
            @NotNull EcmaVersion ecmaVersion,
            boolean sourcemap,
            boolean inlineEnabled,
            boolean metaInfo
    ) {
        this.project = project;
        this.target = ecmaVersion;
        this.moduleId = moduleId;
        this.sourcemap = sourcemap;
        this.inlineEnabled = inlineEnabled;
        this.metaInfo = metaInfo;
    }

    public boolean isSourcemap() {
        return sourcemap;
    }

    public boolean isMetaInfo() {
        return metaInfo;
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

    public abstract boolean checkLibFilesAndReportErrors(@NotNull Function1<String, Unit> report);

    protected abstract void init(@NotNull List<KtFile> sourceFilesInLibraries, @NotNull List<KotlinJavascriptMetadata> metadata);

    @NotNull
    public List<ModuleDescriptorImpl> getModuleDescriptors() {
        init();
        if (moduleDescriptors != null) return moduleDescriptors;

        moduleDescriptors = new SmartList<ModuleDescriptorImpl>();
        for (KotlinJavascriptMetadata metadataEntry : metadata) {
            moduleDescriptors.add(createModuleDescriptor(metadataEntry));
        }
        for (ModuleDescriptorImpl module : moduleDescriptors) {
            setDependencies(module, moduleDescriptors);
        }

        return moduleDescriptors;
    }

    @NotNull
    public List<KtFile> getSourceFilesFromLibraries() {
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

    private ModuleDescriptorImpl createModuleDescriptor(KotlinJavascriptMetadata metadata) {
        assert metadata.isAbiVersionCompatible() :
                "expected abi version " + KotlinJavascriptMetadataUtils.ABI_VERSION +
                ", but metadata.abiVersion = " + metadata.getAbiVersion();

        ModuleDescriptorImpl moduleDescriptor = TargetPlatformKt.createModule(
                JsPlatform.INSTANCE$, Name.special("<" + metadata.getModuleName() + ">"), storageManager
        );

        PackageFragmentProvider provider =
                KotlinJavascriptSerializationUtil.createPackageFragmentProvider(moduleDescriptor, metadata.getBody(), storageManager);

        moduleDescriptor.initialize(provider != null ? provider : PackageFragmentProvider.Empty.INSTANCE$);

        return moduleDescriptor;
    }

    private static void setDependencies(ModuleDescriptorImpl module, List<ModuleDescriptorImpl> modules) {
        module.setDependencies(CollectionsKt.plus(modules, JsPlatform.INSTANCE$.getBuiltIns().getBuiltInsModule()));
    }

    @NotNull
    public static Collection<KtFile> withJsLibAdded(@NotNull Collection<KtFile> files, @NotNull Config config) {
        Collection<KtFile> allFiles = Lists.newArrayList();
        allFiles.addAll(files);
        allFiles.addAll(config.getSourceFilesFromLibraries());
        return allFiles;
    }
}
