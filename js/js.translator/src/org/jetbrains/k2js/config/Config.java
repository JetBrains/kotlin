/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.k2js.config;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.translate.test.JSTester;
import org.jetbrains.k2js.translate.test.QUnitTester;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Base class representing a configuration of translator.
 */
public abstract class Config {
    //NOTE: a hacky solution to be able to rerun code samples with lib loaded only once: used by tests and web demo
    @NotNull
    public static final String REWRITABLE_MODULE_NAME = "JS_TESTS";

    @NotNull
    public static Config getEmptyConfig(@NotNull Project project, @NotNull EcmaVersion ecmaVersion) {
        return new Config(project, "main", ecmaVersion) {
            @NotNull
            @Override
            protected List<JetFile> generateLibFiles() {
                return Collections.emptyList();
            }
        };
    }

    //NOTE: used by mvn build
    @SuppressWarnings("UnusedDeclaration")
    @NotNull
    public static Config getEmptyConfig(@NotNull Project project) {
        return getEmptyConfig(project, EcmaVersion.defaultVersion());
    }

    @NotNull
    public static final List<String> LIB_FILES_WITH_DECLARATIONS = Arrays.asList(
            "/core/annotations.kt",
            "/core/core.kt",
            "/core/date.kt",
            "/core/dom.kt",
            "/core/javaio.kt",
            "/core/javalang.kt",
            "/core/javautil.kt",
            "/core/javautilCollections.kt",
            "/core/json.kt",
            "/core/kotlin.kt",
            "/core/math.kt",
            "/core/string.kt",
            "/core/htmlDom.kt",
            "/html5/canvas.kt",
            "/jquery/common.kt",
            "/jquery/ui.kt",
            "/junit/core.kt",
            "/qunit/core.kt",
            "/stdlib/browser.kt"
    );

    @NotNull
    public static final List<String> LIB_FILES_WITH_CODE = Arrays.asList(
            "/stdlib/TuplesCode.kt",
            "/core/javautilCollectionsCode.kt"
    );

    @NotNull
    public static final List<String> LIB_FILE_NAMES = Lists.newArrayList();

    static {
        LIB_FILE_NAMES.addAll(LIB_FILES_WITH_DECLARATIONS);
        LIB_FILE_NAMES.addAll(LIB_FILES_WITH_CODE);
    }

    /**
     * the library files which depend on the STDLIB files to be able to compile
     */
    @NotNull
    public static final List<String> LIB_FILE_NAMES_DEPENDENT_ON_STDLIB = Arrays.asList(
            "/core/stringsCode.kt",
            "/stdlib/domCode.kt",
            "/stdlib/jutilCode.kt",
            "/stdlib/JUMapsCode.kt",
            "/stdlib/testCode.kt"
    );

    public static final String LIBRARIES_LOCATION = "js/js.libraries/src";

    /**
     * The file names in the standard library to compile
     */
    @NotNull
    public static final List<String> STDLIB_FILE_NAMES = Arrays.asList(
            "/kotlin/Preconditions.kt",
            "/kotlin/Iterators.kt",
            "/kotlin/JUtil.kt",
            "/kotlin/Arrays.kt",
            "/kotlin/Lists.kt",
            "/kotlin/Maps.kt",
            "/kotlin/Exceptions.kt",
            "/kotlin/IterablesSpecial.kt",
            "/generated/_Arrays.kt",
            "/generated/_Collections.kt",
            "/generated/_Iterables.kt",
            "/generated/_Iterators.kt",
            "/kotlin/support/AbstractIterator.kt",
            "/kotlin/Standard.kt",
            "/kotlin/Strings.kt",
            "/kotlin/dom/Dom.kt",
            "/kotlin/test/Test.kt"
    );

    /**
     * The location of the stdlib sources
     */
    public static final String STDLIB_LOCATION = "libraries/stdlib/src";

    @NotNull
    private final Project project;
    @Nullable
    private List<JetFile> libFiles = null;
    @NotNull
    private final EcmaVersion target;

    @NotNull
    private final String moduleId;

    private final boolean sourcemap;

    public Config(@NotNull Project project, @NotNull String moduleId, @NotNull EcmaVersion ecmaVersion) {
        this(project, moduleId, ecmaVersion, false);
    }

    public Config(@NotNull Project project, @NotNull String moduleId, @NotNull EcmaVersion ecmaVersion, boolean sourcemap) {
        this.project = project;
        this.target = ecmaVersion;
        this.moduleId = moduleId;
        this.sourcemap = sourcemap;
    }

    public boolean isSourcemap() {
        return sourcemap;
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

    //TODO: should be null by default I suppose but we can't communicate it to K2JSCompiler atm
    @Nullable
    public JSTester getTester() {
        return new QUnitTester();
    }
}
