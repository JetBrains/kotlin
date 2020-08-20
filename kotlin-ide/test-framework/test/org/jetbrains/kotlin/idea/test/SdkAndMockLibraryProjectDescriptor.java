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
import com.intellij.testFramework.IdeaTestUtil;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts;
import org.jetbrains.kotlin.idea.framework.JSLibraryKind;
import org.jetbrains.kotlin.idea.framework.KotlinSdkType;
import org.jetbrains.kotlin.test.KotlinCompilerStandalone;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;

/* Use MockLibraryFacility instead. */
@Deprecated
public class SdkAndMockLibraryProjectDescriptor extends KotlinLightProjectDescriptor {
    public static final String MOCK_LIBRARY_NAME = "myKotlinLib";

    private final boolean withSources;
    private final boolean withRuntime;
    private final boolean isJsLibrary;
    private final boolean allowKotlinPackage;
    private final String sourcesPath;
    private final List<String> classpath;

    public SdkAndMockLibraryProjectDescriptor(@NotNull String sourcesPath, boolean withSources) {
        this(sourcesPath, withSources, false, false, false);
    }

    public SdkAndMockLibraryProjectDescriptor(
            @NotNull String sourcesPath,
            boolean withSources,
            boolean withRuntime,
            boolean isJsLibrary,
            boolean allowKotlinPackage
    ) {
        this(sourcesPath, withSources, withRuntime, isJsLibrary, allowKotlinPackage, emptyList());
    }

    public SdkAndMockLibraryProjectDescriptor(
            @NotNull String sourcesPath,
            boolean withSources,
            boolean withRuntime,
            boolean isJsLibrary,
            boolean allowKotlinPackage,
            @NotNull List<String> classpath
    ) {
        this.withSources = withSources;
        this.withRuntime = withRuntime;
        this.isJsLibrary = isJsLibrary;
        this.allowKotlinPackage = allowKotlinPackage;
        this.sourcesPath = sourcesPath;
        this.classpath = classpath;
    }

    @Override
    public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model) {
        String jarUrl = getJarUrl(compileLibrary());

        LibraryTable.ModifiableModel libraryTableModel = model.getModuleLibraryTable().getModifiableModel();

        Library.ModifiableModel mockLibraryModel = libraryTableModel.createLibrary(MOCK_LIBRARY_NAME).getModifiableModel();
        mockLibraryModel.addRoot(jarUrl, OrderRootType.CLASSES);

        if (withRuntime && !isJsLibrary) {
            mockLibraryModel.addRoot(getJarUrl(KotlinArtifacts.getInstance().getKotlinStdlib()), OrderRootType.CLASSES);
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
        KotlinCompilerStandalone.Platform platform = isJsLibrary
            ? new KotlinCompilerStandalone.Platform.JavaScript(MOCK_LIBRARY_NAME, "test")
            : new KotlinCompilerStandalone.Platform.Jvm();
        List<File> sources = Collections.singletonList(new File(sourcesPath));
        List<File> classpath = isJsLibrary ? emptyList() : CollectionsKt.map(this.classpath, File::new);
        File libraryJar = KotlinCompilerStandalone.defaultTargetJar();
        new KotlinCompilerStandalone(sources, libraryJar, platform, extraOptions, classpath).compile();
        return libraryJar;
    }

    @Override
    public Sdk getSdk() {
        return isJsLibrary ? KotlinSdkType.INSTANCE.createSdkWithUniqueName(emptyList()) : IdeaTestUtil.getMockJdk18();
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
        return withSources == that.withSources &&
               withRuntime == that.withRuntime &&
               isJsLibrary == that.isJsLibrary &&
               allowKotlinPackage == that.allowKotlinPackage &&
               sourcesPath.equals(that.sourcesPath) &&
               classpath.equals(that.classpath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(withSources, withRuntime, isJsLibrary, allowKotlinPackage, sourcesPath, classpath);
    }
}
