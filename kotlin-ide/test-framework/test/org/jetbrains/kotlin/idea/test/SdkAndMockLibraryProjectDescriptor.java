/*
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

package org.jetbrains.kotlin.idea.test;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.io.FileUtilRt;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.artifacts.TestKotlinArtifacts;
import org.jetbrains.kotlin.idea.framework.JSLibraryKind;
import org.jetbrains.kotlin.idea.framework.KotlinSdkType;
import org.jetbrains.kotlin.platform.SimplePlatform;
import org.jetbrains.kotlin.test.KotlinCompilerStandalone;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;

public class SdkAndMockLibraryProjectDescriptor extends KotlinLightProjectDescriptor {
    public static final String MOCK_LIBRARY_NAME = "myKotlinLib";

    private final String sourcesPath;
    private final boolean withSources;
    private final boolean withRuntime;
    private final boolean isJsLibrary;
    private final boolean allowKotlinPackage;
    private final List<String> classpath;

    public SdkAndMockLibraryProjectDescriptor(String sourcesPath, boolean withSources) {
        this(sourcesPath, withSources, false, false, false);
    }

    public SdkAndMockLibraryProjectDescriptor(
            String sourcesPath,
            boolean withSources,
            boolean withRuntime,
            boolean isJsLibrary,
            boolean allowKotlinPackage
    ) {
        this(sourcesPath, withSources, withRuntime, isJsLibrary, allowKotlinPackage, emptyList());
    }

    public SdkAndMockLibraryProjectDescriptor(
            String sourcesPath,
            boolean withSources,
            boolean withRuntime,
            boolean isJsLibrary,
            boolean allowKotlinPackage,
            List<String> classpath
    ) {
        this.sourcesPath = sourcesPath;
        this.withSources = withSources;
        this.withRuntime = withRuntime;
        this.isJsLibrary = isJsLibrary;
        this.allowKotlinPackage = allowKotlinPackage;
        this.classpath = classpath;
    }

    @Override
    public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model) {
        String jarUrl = getJarUrl(compileLibrary());

        LibraryTable.ModifiableModel libraryTableModel = model.getModuleLibraryTable().getModifiableModel();

        Library.ModifiableModel mockLibraryModel = libraryTableModel.createLibrary(MOCK_LIBRARY_NAME).getModifiableModel();
        mockLibraryModel.addRoot(jarUrl, OrderRootType.CLASSES);

        if (withRuntime && !isJsLibrary) {
            mockLibraryModel.addRoot(getJarUrl(TestKotlinArtifacts.INSTANCE.getKotlinStdlib()), OrderRootType.CLASSES);
        }

        if (isJsLibrary && mockLibraryModel instanceof LibraryEx.ModifiableModelEx) {
            ((LibraryEx.ModifiableModelEx) mockLibraryModel).setKind(JSLibraryKind.INSTANCE);
        }

        if (withSources) {
            mockLibraryModel.addRoot(jarUrl + "src/", OrderRootType.SOURCES);
        }

        mockLibraryModel.commit();

        if (withRuntime && isJsLibrary) {
            KotlinStdJSProjectDescriptor.INSTANCE.configureModule(module, model);
        }
    }

    private File compileLibrary() {
        List<String> extraOptions = allowKotlinPackage ? Collections.singletonList("-Xallow-kotlin-package") : emptyList();
        SimplePlatform platform = isJsLibrary ? KotlinCompilerStandalone.getJsPlatform() : KotlinCompilerStandalone.getJvmPlatform();
        List<File> sources = Collections.singletonList(new File(sourcesPath));
        List<File> classpath = isJsLibrary ? emptyList() : CollectionsKt.map(this.classpath, File::new);
        File libraryJar = KotlinCompilerStandalone.defaultTargetJar();
        new KotlinCompilerStandalone(sources, libraryJar, platform, extraOptions, classpath).compile();
        return libraryJar;
    }

    @Override
    public Sdk getSdk() {
        return isJsLibrary ? KotlinSdkType.INSTANCE.createSdkWithUniqueName(emptyList()) : PluginTestCaseBase.mockJdk();
    }

    @NotNull
    private static String getJarUrl(@NotNull File libraryJar) {
        return "jar://" + FileUtilRt.toSystemIndependentName(libraryJar.getAbsolutePath()) + "!/";
    }

    public static void tearDown(Module module) {
        ConfigLibraryUtil.INSTANCE.removeLibrary(module, SdkAndMockLibraryProjectDescriptor.MOCK_LIBRARY_NAME);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SdkAndMockLibraryProjectDescriptor that = (SdkAndMockLibraryProjectDescriptor) o;

        if (withSources != that.withSources) return false;
        if (withRuntime != that.withRuntime) return false;
        if (isJsLibrary != that.isJsLibrary) return false;
        if (!sourcesPath.equals(that.sourcesPath)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = sourcesPath.hashCode();
        result = 31 * result + (withSources ? 1 : 0);
        result = 31 * result + (withRuntime ? 1 : 0);
        result = 31 * result + (isJsLibrary ? 1 : 0);
        return result;
    }
}
