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

package org.jetbrains.kotlin.jps.build;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.utils.LibraryUtils;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.model.java.JpsJavaClasspathKind;
import org.jetbrains.jps.model.java.JpsJavaDependenciesEnumerator;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsLibraryRoot;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.util.JpsPathUtil;

import java.util.Set;

class JpsUtils {
    private JpsUtils() {}

    @NotNull
    static JpsJavaDependenciesEnumerator getAllDependencies(@NotNull ModuleBuildTarget target) {
        return JpsJavaExtensionService.dependencies(target.getModule()).recursively().exportedOnly()
                .includedIn(JpsJavaClasspathKind.compile(target.isTests()));
    }

    static boolean isJsKotlinModule(@NotNull ModuleBuildTarget target) {
        Set<JpsLibrary> libraries = getAllDependencies(target).getLibraries();
        for (JpsLibrary library : libraries) {
            for (JpsLibraryRoot root : library.getRoots(JpsOrderRootType.COMPILED)) {
                if (LibraryUtils.isKotlinJavascriptStdLibrary(JpsPathUtil.urlToFile(root.getUrl())))
                    return true;
            }
        }
        return false;
    }
}
