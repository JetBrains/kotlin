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

import com.intellij.openapi.project.Project;
import com.intellij.util.SmartList;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.incremental.components.LookupTracker;
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration;
import org.jetbrains.kotlin.resolve.TargetEnvironment;
import org.jetbrains.kotlin.serialization.js.*;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.utils.JsMetadataVersion;
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadata;
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils;

import java.io.File;
import java.util.*;

import static org.jetbrains.kotlin.utils.PathUtil.getKotlinPathsForDistDirectory;

public class JsConfig {
    public static final List<String> JS_STDLIB =
            Collections.singletonList(getKotlinPathsForDistDirectory().getJsStdLibKlibPath().getAbsolutePath());

    public static final List<String> JS_KOTLIN_TEST =
            Collections.singletonList(getKotlinPathsForDistDirectory().getJsKotlinTestKlibPath().getAbsolutePath());

    public static final String UNKNOWN_EXTERNAL_MODULE_NAME = "<unknown>";

    private final Project project;
    private final CompilerConfiguration configuration;
    private final LockBasedStorageManager storageManager = new LockBasedStorageManager("JsConfig");

    private final List<KotlinJavascriptMetadata> metadata = new SmartList<>();
    private final List<KotlinJavascriptMetadata> friends = new SmartList<>();

    private List<ModuleDescriptorImpl> moduleDescriptors;
    private List<ModuleDescriptorImpl> friendModuleDescriptors;

    private boolean initialized = false;

    @Nullable
    private final List<JsModuleDescriptor<KotlinJavaScriptLibraryParts>> metadataCache;

    @Nullable
    private final Set<String> librariesToSkip;

    public final TargetEnvironment targetEnvironment;

    public JsConfig(@NotNull Project project, @NotNull CompilerConfiguration configuration, @NotNull TargetEnvironment targetEnvironment) {
        this(project, configuration, targetEnvironment, null, null);
    }

    public JsConfig(
            @NotNull Project project,
            @NotNull CompilerConfiguration configuration,
            @NotNull TargetEnvironment targetEnvironment,
            @Nullable List<JsModuleDescriptor<KotlinJavaScriptLibraryParts>> metadataCache,
            @Nullable Set<String> librariesToSkip
    ) {
        this.project = project;
        this.configuration = configuration.copy();
        this.targetEnvironment = targetEnvironment;
        this.metadataCache = metadataCache;
        this.librariesToSkip = librariesToSkip;
    }

    @NotNull
    public CompilerConfiguration getConfiguration() {
        return configuration;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public String getModuleId() {
        return configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME);
    }

    @NotNull
    public ModuleKind getModuleKind() {
        return configuration.get(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN);
    }

    @NotNull
    public List<String> getLibraries() {
        return getConfiguration().getList(JSConfigurationKeys.LIBRARIES);
    }

    @NotNull
    public String getSourceMapPrefix() {
        return configuration.get(JSConfigurationKeys.SOURCE_MAP_PREFIX, "");
    }

    @NotNull
    public List<String> getSourceMapRoots() {
        return configuration.get(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, Collections.emptyList());
    }

    public boolean shouldGenerateRelativePathsInSourceMap() {
        return getSourceMapPrefix().isEmpty() && getSourceMapRoots().isEmpty();
    }

    @NotNull
    public SourceMapSourceEmbedding getSourceMapContentEmbedding() {
        return configuration.get(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES, SourceMapSourceEmbedding.INLINING);
    }

    @NotNull
    public List<String> getFriends() {
        if (getConfiguration().getBoolean(JSConfigurationKeys.FRIEND_PATHS_DISABLED)) return Collections.emptyList();
        return getConfiguration().getList(JSConfigurationKeys.FRIEND_PATHS);
    }

    @NotNull
    public LanguageVersionSettings getLanguageVersionSettings() {
        return CommonConfigurationKeysKt.getLanguageVersionSettings(configuration);
    }

    public boolean isAtLeast(@NotNull LanguageVersion expected) {
        LanguageVersion actual = CommonConfigurationKeysKt.getLanguageVersionSettings(configuration).getLanguageVersion();
        return actual.getMajor() > expected.getMajor() ||
               actual.getMajor() == expected.getMajor() && actual.getMinor() >= expected.getMinor();
    }


    public static abstract class Reporter {
        public void error(@NotNull String message) { /*Do nothing*/ }

        public void warning(@NotNull String message) { /*Do nothing*/ }
    }

    public boolean checkLibFilesAndReportErrors(@NotNull JsConfig.Reporter report) {
        return checkLibFilesAndReportErrors(getLibraries(), report);
    }

    private boolean checkLibFilesAndReportErrors(
            @NotNull Collection<String> libraries,
            @NotNull JsConfig.Reporter report
    ) {
        if (libraries.isEmpty()) {
            return false;
        }

        Set<String> modules = new HashSet<>();

        boolean skipMetadataVersionCheck = getLanguageVersionSettings().getFlag(AnalysisFlags.getSkipMetadataVersionCheck());

        for (String path : libraries) {
            if (librariesToSkip != null && librariesToSkip.contains(path)) continue;

            File filePath = new File(path);
            if (!filePath.exists()) {
                report.error("Path '" + path + "' does not exist");
                return true;
            }

            List<KotlinJavascriptMetadata> metadataList = KotlinJavascriptMetadataUtils.loadMetadata(path);
            if (metadataList.isEmpty()) {
                report.warning("'" + path + "' is not a valid Kotlin Javascript library");
                continue;
            }

            Set<String> moduleNames = new LinkedHashSet<>();

            for (KotlinJavascriptMetadata metadata : metadataList) {
                if (!metadata.getVersion().isCompatibleWithCurrentCompilerVersion() && !skipMetadataVersionCheck) {
                    report.error("File '" + path + "' was compiled with an incompatible version of Kotlin. " +
                                 "The binary version of its metadata is " + metadata.getVersion() +
                                 ", expected version is " + JsMetadataVersion.INSTANCE);
                    return true;
                }

                moduleNames.add(metadata.getModuleName());
            }

            for (String moduleName : moduleNames) {
                if (!modules.add(moduleName)) {
                    report.warning("Module \"" + moduleName + "\" is defined in more than one file");
                }
            }

            if (modules.contains(getModuleId())) {
                report.warning("Module \"" + getModuleId() + "\" depends on module with the same name");
            }

            Set<String> friendLibsSet = new HashSet<>(getFriends());
            metadata.addAll(metadataList);
            if (friendLibsSet.contains(path)){
                friends.addAll(metadataList);
            }
        }

        initialized = true;
        return false;
    }

    @NotNull
    public List<ModuleDescriptorImpl> getModuleDescriptors() {
        init();
        return moduleDescriptors;
    }

    @NotNull
    private List<ModuleDescriptorImpl> createModuleDescriptors() {
        List<ModuleDescriptorImpl> moduleDescriptors = new SmartList<>();
        for (KotlinJavascriptMetadata metadataEntry : metadata) {
            moduleDescriptors.add(createModuleDescriptor(metadataEntry));
        }

        if (metadataCache != null) {
            LanguageVersionSettings languageVersionSettings = CommonConfigurationKeysKt.getLanguageVersionSettings(configuration);
            for (JsModuleDescriptor<KotlinJavaScriptLibraryParts> cached : metadataCache) {
                ModuleDescriptorImpl moduleDescriptor = new ModuleDescriptorImpl(
                        Name.special("<" + cached.getName() + ">"), storageManager, JsPlatformAnalyzerServices.INSTANCE.getBuiltIns()
                );

                KotlinJavaScriptLibraryParts parts = cached.getData();
                PackageFragmentProvider provider = KotlinJavascriptPackageFragmentProviderKt.createKotlinJavascriptPackageFragmentProvider(
                        storageManager, moduleDescriptor, parts.getHeader(), parts.getBody(), parts.getMetadataVersion(),
                        new CompilerDeserializationConfiguration(languageVersionSettings),
                        LookupTracker.DO_NOTHING.INSTANCE
                );

                moduleDescriptor.initialize(provider);
                moduleDescriptors.add(moduleDescriptor);
            }
        }

        for (ModuleDescriptorImpl module : moduleDescriptors) {
            setDependencies(module, moduleDescriptors);
        }

        return Collections.unmodifiableList(moduleDescriptors);
    }

    @NotNull
    public List<ModuleDescriptorImpl> getFriendModuleDescriptors() {
        init();
        return friendModuleDescriptors;
    }

    public void init() {
        if (!initialized) {
            JsConfig.Reporter reporter = new Reporter() {
                @Override
                public void error(@NotNull String message) {
                    throw new IllegalStateException(message);
                }
            };

            checkLibFilesAndReportErrors(reporter);
        }

        if (moduleDescriptors == null) {
            moduleDescriptors = createModuleDescriptors();
        }

        if (friendModuleDescriptors == null) {
            friendModuleDescriptors = CollectionsKt.map(friends, this::createModuleDescriptor);
        }
    }

    private final IdentityHashMap<KotlinJavascriptMetadata, ModuleDescriptorImpl> factoryMap = new IdentityHashMap<>();

    private ModuleDescriptorImpl createModuleDescriptor(KotlinJavascriptMetadata metadata) {
        return factoryMap.computeIfAbsent(metadata, m -> {
            LanguageVersionSettings languageVersionSettings = CommonConfigurationKeysKt.getLanguageVersionSettings(configuration);
            assert m.getVersion().isCompatibleWithCurrentCompilerVersion() ||
                   languageVersionSettings.getFlag(AnalysisFlags.getSkipMetadataVersionCheck()) :
                    "Expected JS metadata version " + JsMetadataVersion.INSTANCE + ", but actual metadata version is " + m.getVersion();

            ModuleDescriptorImpl moduleDescriptor = new ModuleDescriptorImpl(
                    Name.special("<" + m.getModuleName() + ">"), storageManager, JsPlatformAnalyzerServices.INSTANCE.getBuiltIns()
            );

            LookupTracker lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER, LookupTracker.DO_NOTHING.INSTANCE);
            KotlinJavaScriptLibraryParts parts = KotlinJavascriptSerializationUtil.readModuleAsProto(m.getBody(), m.getVersion());
            PackageFragmentProvider provider = KotlinJavascriptPackageFragmentProviderKt.createKotlinJavascriptPackageFragmentProvider(
                    storageManager, moduleDescriptor, parts.getHeader(), parts.getBody(), m.getVersion(),
                    new CompilerDeserializationConfiguration(languageVersionSettings),
                    lookupTracker
            );

            moduleDescriptor.initialize(provider);
            return moduleDescriptor;
        });
    }

    private static void setDependencies(ModuleDescriptorImpl module, List<ModuleDescriptorImpl> modules) {
        module.setDependencies(CollectionsKt.plus(modules, JsPlatformAnalyzerServices.INSTANCE.getBuiltIns().getBuiltInsModule()));
    }
}
