/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration;

import com.intellij.jarRepository.RepositoryLibraryType;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.RootPolicy;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaModule;
import com.intellij.psi.PsiRequiresStatement;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.arguments.InternalArgument;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifactNames;
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder;
import org.jetbrains.kotlin.idea.facet.FacetUtilsKt;
import org.jetbrains.kotlin.idea.facet.KotlinFacet;
import org.jetbrains.kotlin.idea.framework.JSLibraryKind;
import org.jetbrains.kotlin.idea.framework.JsLibraryStdDetectionUtil;
import org.jetbrains.kotlin.idea.framework.LibraryEffectiveKindProviderKt;
import org.jetbrains.kotlin.idea.project.PlatformKt;
import org.jetbrains.kotlin.idea.util.Java9StructureUtilKt;
import org.jetbrains.kotlin.idea.versions.KotlinRuntimeLibraryUtilKt;
import org.jetbrains.kotlin.config.JvmTarget;
import org.jetbrains.kotlin.platform.TargetPlatform;
import org.jetbrains.kotlin.platform.js.JsPlatforms;
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms;
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleKt;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@RunWith(JUnit38ClassRunner.class)
public class ConfigureKotlinTest extends AbstractConfigureKotlinTest {
    public void testNewLibrary_copyJar() {
        doTestSingleJvmModule(KotlinWithLibraryConfigurator.FileState.COPY);

        ModuleRootManager.getInstance(getModule()).orderEntries().forEachLibrary(library -> {
            assertSameElements(
                    Arrays.stream(library.getRootProvider().getFiles(OrderRootType.CLASSES)).map(VirtualFile::getName).toArray(),
                    KotlinArtifactNames.KOTLIN_STDLIB, KotlinArtifactNames.KOTLIN_REFLECT, KotlinArtifactNames.KOTLIN_TEST);

            assertSameElements(
                    Arrays.stream(library.getRootProvider().getFiles(OrderRootType.SOURCES)).map(VirtualFile::getName).toArray(),
                    KotlinArtifactNames.KOTLIN_STDLIB, KotlinArtifactNames.KOTLIN_REFLECT, KotlinArtifactNames.KOTLIN_TEST);

            return true;
        });
    }

    public void testNewLibrary_doNotCopyJar() {
        doTestSingleJvmModule(KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY);
    }

    public void testLibraryWithoutPaths_jarExists() {
        doTestSingleJvmModule(KotlinWithLibraryConfigurator.FileState.EXISTS);
    }

    public void testNewLibrary_jarExists() {
        doTestSingleJvmModule(KotlinWithLibraryConfigurator.FileState.EXISTS);
    }

    public void testLibraryWithoutPaths_copyJar() {
        doTestSingleJvmModule(KotlinWithLibraryConfigurator.FileState.COPY);
    }

    public void testLibraryWithoutPaths_doNotCopyJar() {
        doTestSingleJvmModule(KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY);
    }

    public void testTwoModules_exists() {
        Module[] modules = getModules();
        for (Module module : modules) {
            if (module.getName().equals("module1")) {
                configure(module, KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY, getJvmConfigurator());
                assertConfigured(module, getJvmConfigurator());
            }
            else if (module.getName().equals("module2")) {
                assertNotConfigured(module, getJvmConfigurator());
                configure(module, KotlinWithLibraryConfigurator.FileState.EXISTS, getJvmConfigurator());
                assertConfigured(module, getJvmConfigurator());
            }
        }
    }

    public void testLibraryNonDefault_libExistInDefault() throws IOException {
        Module module = getModule();

        // Move fake runtime jar to default library path to pretend library is already configured
        FileUtil.copy(
                new File(getProject().getBasePath() + "/lib/" + KotlinArtifactNames.KOTLIN_STDLIB),
                new File(getJvmConfigurator().getDefaultPathToJarFile(getProject()) + "/" + KotlinArtifactNames.KOTLIN_STDLIB));

        assertNotConfigured(module, getJvmConfigurator());
        getJvmConfigurator().configure(myProject, emptyList());
        assertProperlyConfigured(module, getJvmConfigurator());
    }

    public void testTwoModulesWithNonDefaultPath_doNotCopyInDefault() {
        doTestConfigureModulesWithNonDefaultSetup(getJvmConfigurator());
        assertEmpty(ConfigureKotlinInProjectUtilsKt.getCanBeConfiguredModules(myProject, getJsConfigurator()));
    }

    public void testTwoModulesWithJSNonDefaultPath_doNotCopyInDefault() {
        doTestConfigureModulesWithNonDefaultSetup(getJsConfigurator());
        assertEmpty(ConfigureKotlinInProjectUtilsKt.getCanBeConfiguredModules(myProject, getJvmConfigurator()));
    }

    public void testNewLibrary_jarExists_js() {
        doTestSingleJsModule(KotlinWithLibraryConfigurator.FileState.EXISTS);
    }

    public void testNewLibrary_copyJar_js() {
        doTestSingleJsModule(KotlinWithLibraryConfigurator.FileState.COPY);
    }

    public void testNewLibrary_doNotCopyJar_js() {
        doTestSingleJsModule(KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY);
    }

    public void testJsLibraryWithoutPaths_jarExists() {
        doTestSingleJsModule(KotlinWithLibraryConfigurator.FileState.EXISTS);
    }

    public void testJsLibraryWithoutPaths_copyJar() {
        doTestSingleJsModule(KotlinWithLibraryConfigurator.FileState.COPY);
    }

    public void testJsLibraryWithoutPaths_doNotCopyJar() {
        doTestSingleJsModule(KotlinWithLibraryConfigurator.FileState.DO_NOT_COPY);
    }

    public void testJsLibraryWrongKind() {
        assertProperlyConfigured(getModule(), getJsConfigurator());
        assertEquals(1, ModuleRootManager.getInstance(getModule()).orderEntries().process(new LibraryCountingRootPolicy(), 0).intValue());
    }

    public void testProjectWithoutFacetWithRuntime106WithoutLanguageLevel() {
        assertEquals(LanguageVersion.KOTLIN_1_0, PlatformKt.getLanguageVersionSettings(getModule()).getLanguageVersion());
        assertEquals(LanguageVersion.KOTLIN_1_0, PlatformKt.getLanguageVersionSettings(myProject, null).getLanguageVersion());
    }

    public void testProjectWithoutFacetWithRuntime11WithoutLanguageLevel() {
        assertEquals(LanguageVersion.KOTLIN_1_1, PlatformKt.getLanguageVersionSettings(getModule()).getLanguageVersion());
        assertEquals(LanguageVersion.KOTLIN_1_1, PlatformKt.getLanguageVersionSettings(myProject, null).getLanguageVersion());
    }

    public void testProjectWithoutFacetWithRuntime11WithLanguageLevel10() {
        assertEquals(LanguageVersion.KOTLIN_1_0, PlatformKt.getLanguageVersionSettings(getModule()).getLanguageVersion());
        assertEquals(LanguageVersion.KOTLIN_1_0, PlatformKt.getLanguageVersionSettings(myProject, null).getLanguageVersion());
    }

    public void testProjectWithFacetWithRuntime11WithLanguageLevel10() {
        assertEquals(LanguageVersion.KOTLIN_1_0, PlatformKt.getLanguageVersionSettings(getModule()).getLanguageVersion());
        assertEquals(
                VersionView.Companion.getRELEASED_VERSION(),
                PlatformKt.getLanguageVersionSettings(myProject, null).getLanguageVersion()
        );
    }

    public void testJsLibraryVersion11() {
        Library jsRuntime = KotlinRuntimeLibraryUtilKt.findAllUsedLibraries(myProject).keySet().iterator().next();
        String version = JsLibraryStdDetectionUtil.INSTANCE.getJsLibraryStdVersion(jsRuntime, myProject);
        assertEquals("1.1.0", version);
    }

    public void testJsLibraryVersion106() {
        Library jsRuntime = KotlinRuntimeLibraryUtilKt.findAllUsedLibraries(myProject).keySet().iterator().next();
        String version = JsLibraryStdDetectionUtil.INSTANCE.getJsLibraryStdVersion(jsRuntime, myProject);
        assertEquals("1.0.6", version);
    }

    public void testMavenProvidedTestJsKind() {
        LibraryEx jsTest = (LibraryEx) ContainerUtil.find(
                KotlinRuntimeLibraryUtilKt.findAllUsedLibraries(myProject).keySet(),
                (library) -> library.getName().contains("kotlin-test-js")
        );
        assertEquals(RepositoryLibraryType.REPOSITORY_LIBRARY_KIND, jsTest.getKind());
        assertEquals(JSLibraryKind.INSTANCE, LibraryEffectiveKindProviderKt.effectiveKind(jsTest, myProject));
    }

    public void testJvmProjectWithV1FacetConfig() {
        KotlinFacetSettings settings = KotlinFacetSettingsProvider.Companion.getInstance(myProject).getInitializedSettings(getModule());
        K2JVMCompilerArguments arguments = (K2JVMCompilerArguments) settings.getCompilerArguments();
        assertFalse(settings.getUseProjectSettings());
        assertEquals(LanguageVersion.KOTLIN_1_1, settings.getLanguageLevel());
        assertEquals(LanguageVersion.KOTLIN_1_0, settings.getApiLevel());
        assertEquals(JvmPlatforms.INSTANCE.getJvm18(), settings.getTargetPlatform());
        assertEquals("1.1", arguments.getLanguageVersion());
        assertEquals("1.0", arguments.getApiVersion());
        assertEquals(LanguageFeature.State.ENABLED_WITH_WARNING, CoroutineSupport.byCompilerArguments(arguments));
        assertEquals("1.7", arguments.getJvmTarget());
        assertEquals("-version -Xallow-kotlin-package -Xskip-metadata-version-check", settings.getCompilerSettings().getAdditionalArguments());
    }

    public void testJsProjectWithV1FacetConfig() {
        KotlinFacetSettings settings = KotlinFacetSettingsProvider.Companion.getInstance(myProject).getInitializedSettings(getModule());
        K2JSCompilerArguments arguments = (K2JSCompilerArguments) settings.getCompilerArguments();
        assertFalse(settings.getUseProjectSettings());
        assertEquals(LanguageVersion.KOTLIN_1_1, settings.getLanguageLevel());
        assertEquals(LanguageVersion.KOTLIN_1_0, settings.getApiLevel());
        assertEquals(JsPlatforms.INSTANCE.getDefaultJsPlatform(), settings.getTargetPlatform());
        assertEquals("1.1", arguments.getLanguageVersion());
        assertEquals("1.0", arguments.getApiVersion());
        assertEquals(LanguageFeature.State.ENABLED_WITH_WARNING, CoroutineSupport.byCompilerArguments(arguments));
        assertEquals("amd", arguments.getModuleKind());
        assertEquals("-version -meta-info", settings.getCompilerSettings().getAdditionalArguments());
    }

    public void testJvmProjectWithV2FacetConfig() {
        KotlinFacetSettings settings = KotlinFacetSettingsProvider.Companion.getInstance(myProject).getInitializedSettings(getModule());
        K2JVMCompilerArguments arguments = (K2JVMCompilerArguments) settings.getCompilerArguments();
        assertFalse(settings.getUseProjectSettings());
        assertEquals(LanguageVersion.KOTLIN_1_1, settings.getLanguageLevel());
        assertEquals(LanguageVersion.KOTLIN_1_0, settings.getApiLevel());
        assertEquals(JvmPlatforms.INSTANCE.getJvm18(), settings.getTargetPlatform());
        assertEquals("1.1", arguments.getLanguageVersion());
        assertEquals("1.0", arguments.getApiVersion());
        assertEquals(LanguageFeature.State.ENABLED, CoroutineSupport.byCompilerArguments(arguments));
        assertEquals("1.7", arguments.getJvmTarget());
        assertEquals("-version -Xallow-kotlin-package -Xskip-metadata-version-check", settings.getCompilerSettings().getAdditionalArguments());
    }

    public void testJsProjectWithV2FacetConfig() {
        KotlinFacetSettings settings = KotlinFacetSettingsProvider.Companion.getInstance(myProject).getInitializedSettings(getModule());
        K2JSCompilerArguments arguments = (K2JSCompilerArguments) settings.getCompilerArguments();
        assertFalse(settings.getUseProjectSettings());
        assertEquals(LanguageVersion.KOTLIN_1_1, settings.getLanguageLevel());
        assertEquals(LanguageVersion.KOTLIN_1_0, settings.getApiLevel());
        assertEquals(JsPlatforms.INSTANCE.getDefaultJsPlatform(), settings.getTargetPlatform());
        assertEquals("1.1", arguments.getLanguageVersion());
        assertEquals("1.0", arguments.getApiVersion());
        assertEquals(LanguageFeature.State.ENABLED_WITH_ERROR, CoroutineSupport.byCompilerArguments(arguments));
        assertEquals("amd", arguments.getModuleKind());
        assertEquals("-version -meta-info", settings.getCompilerSettings().getAdditionalArguments());
    }

    public void testJvmProjectWithV3FacetConfig() {
        KotlinFacetSettings settings = KotlinFacetSettingsProvider.Companion.getInstance(myProject).getInitializedSettings(getModule());
        K2JVMCompilerArguments arguments = (K2JVMCompilerArguments) settings.getCompilerArguments();
        assertFalse(settings.getUseProjectSettings());
        assertEquals(LanguageVersion.KOTLIN_1_1, settings.getLanguageLevel());
        assertEquals(LanguageVersion.KOTLIN_1_0, settings.getApiLevel());
        assertEquals(JvmPlatforms.INSTANCE.getJvm18(), settings.getTargetPlatform());
        assertEquals("1.1", arguments.getLanguageVersion());
        assertEquals("1.0", arguments.getApiVersion());
        assertEquals(LanguageFeature.State.ENABLED, CoroutineSupport.byCompilerArguments(arguments));
        assertEquals("1.7", arguments.getJvmTarget());
        assertEquals("-version -Xallow-kotlin-package -Xskip-metadata-version-check", settings.getCompilerSettings().getAdditionalArguments());
    }

    public void testJvmProjectWithJvmTarget11() {
        KotlinFacetSettings settings = KotlinFacetSettingsProvider.Companion.getInstance(myProject).getInitializedSettings(getModule());
        assertEquals(JvmPlatforms.INSTANCE.jvmPlatformByTargetVersion(JvmTarget.JVM_11), settings.getTargetPlatform());
    }

    public void testImplementsDependency() {
        ModuleManager moduleManager = ModuleManager.getInstance(myProject);

        Module module1 = moduleManager.findModuleByName("module1");
        assert module1 != null;

        Module module2 = moduleManager.findModuleByName("module2");
        assert module2 != null;

        assertEquals(emptyList(), KotlinFacet.Companion.get(module1).getConfiguration().getSettings().getImplementedModuleNames());
        assertEquals(singletonList("module1"), KotlinFacet.Companion.get(module2).getConfiguration().getSettings().getImplementedModuleNames());
    }

    public void testJava9WithModuleInfo() {
        checkAddStdlibModule();
    }

    public void testJava9WithModuleInfoWithStdlibAlready() {
        checkAddStdlibModule();
    }

    public void testProjectWithFreeArgs() {
        assertEquals(singletonList("true"), KotlinCommonCompilerArgumentsHolder.Companion.getInstance(myProject).getSettings().getFreeArgs());
    }

    public void testProjectWithInternalArgs() {
        List<InternalArgument> internalArguments =
                KotlinCommonCompilerArgumentsHolder.Companion.getInstance(myProject).getSettings().getInternalArguments();
        assertEquals(
                0,
                internalArguments.size()
        );
    }

    private void checkAddStdlibModule() {
        doTestSingleJvmModule(KotlinWithLibraryConfigurator.FileState.COPY);

        Module module = getModule();
        Sdk moduleSdk = ModuleRootManager.getInstance(getModule()).getSdk();
        assertNotNull("Module SDK is not defined", moduleSdk);

        PsiJavaModule javaModule = Java9StructureUtilKt.findFirstPsiJavaModule(module);
        assertNotNull(javaModule);

        PsiRequiresStatement stdlibDirective =
                Java9StructureUtilKt.findRequireDirective(javaModule, JavaModuleKt.KOTLIN_STDLIB_MODULE_NAME);
        assertNotNull("Require directive for " + JavaModuleKt.KOTLIN_STDLIB_MODULE_NAME + " is expected",
                      stdlibDirective);

        long numberOfStdlib = StreamSupport.stream(javaModule.getRequires().spliterator(), false)
                .filter((statement) -> JavaModuleKt.KOTLIN_STDLIB_MODULE_NAME.equals(statement.getModuleName()))
                .count();

        assertEquals("Only one standard library directive is expected", 1, numberOfStdlib);
    }

    private void configureFacetAndCheckJvm(JvmTarget jvmTarget) {
        IdeModifiableModelsProviderImpl modelsProvider = new IdeModifiableModelsProviderImpl(getProject());
        try {
            KotlinFacet facet = FacetUtilsKt.getOrCreateFacet(getModule(), modelsProvider, false, null, false);
            TargetPlatform platform = JvmPlatforms.INSTANCE.jvmPlatformByTargetVersion(jvmTarget);
            FacetUtilsKt.configureFacet(
                    facet,
                    "1.1",
                    LanguageFeature.State.ENABLED,
                    platform,
                    modelsProvider
            );
            assertEquals(platform, facet.getConfiguration().getSettings().getTargetPlatform());
            assertEquals(jvmTarget.getDescription(),
                         ((K2JVMCompilerArguments) facet.getConfiguration().getSettings().getCompilerArguments()).getJvmTarget());
        }
        finally {
            modelsProvider.dispose();
        }
    }

    public void testJvm8InProjectJvm6InModule() {
        configureFacetAndCheckJvm(JvmTarget.JVM_1_6);
    }

    public void testJvm6InProjectJvm8InModule() {
        configureFacetAndCheckJvm(JvmTarget.JVM_1_8);
    }

    public void testProjectWithoutFacetWithJvmTarget18() {
        assertEquals(JvmPlatforms.INSTANCE.getJvm18(), PlatformKt.getPlatform(getModule()));
    }

    private static class LibraryCountingRootPolicy extends RootPolicy<Integer> {
        @Override
        public Integer visitLibraryOrderEntry(@NotNull LibraryOrderEntry libraryOrderEntry, Integer value) {
            return value + 1;
        }
    }
}
