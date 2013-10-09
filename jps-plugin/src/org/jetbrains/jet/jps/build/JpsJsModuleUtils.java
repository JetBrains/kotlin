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

package org.jetbrains.jet.jps.build;

import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.utils.LibraryUtils;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.java.JpsJavaModuleType;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsLibraryRoot;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;
import org.jetbrains.jps.util.JpsPathUtil;

import java.util.HashSet;
import java.util.Set;

class JpsJsModuleUtils {
    private JpsJsModuleUtils() {}

    public static String urlToOsPath(String url) {
        return FileUtilRt.toSystemDependentName(JpsPathUtil.urlToPath(url));
    }

    @NotNull
    static Set<String> getLibraryFilesAndDependencies(@NotNull ModuleBuildTarget target) {
        Set<String> result = new HashSet<String>();
        getLibraryFiles(target, result);
        getDependencyModulesAndSources(target, result);
        return result;
    }

    static void getLibraryFiles(@NotNull ModuleBuildTarget target, @NotNull Set<String> result) {
        Set<JpsLibrary> libraries = JpsUtils.getAllDependencies(target).getLibraries();
        for (JpsLibrary library : libraries) {
            for (JpsLibraryRoot root : library.getRoots(JpsOrderRootType.COMPILED)) {
                String path = urlToOsPath(root.getUrl());
                // TODO: Do we need to add to dependency all libraries?
                if (LibraryUtils.isJsRuntimeLibrary(JpsPathUtil.urlToFile(path))) {
                    result.add(path);
                }
            }
        }
    }

    static void getDependencyModulesAndSources(@NotNull final ModuleBuildTarget target, @NotNull final Set<String> result) {
        JpsUtils.getAllDependencies(target).processModules(new Consumer<JpsModule>() {
            @Override
            public void consume(JpsModule module) {
                if (module == target.getModule() || module.getModuleType() != JpsJavaModuleType.INSTANCE) return;

                result.add("@" + module.getName());

                for (JpsModuleSourceRoot root : module.getSourceRoots(JavaSourceRootType.SOURCE)) {
                    result.add(urlToOsPath(root.getUrl()));
                }
            }
        });
    }
}