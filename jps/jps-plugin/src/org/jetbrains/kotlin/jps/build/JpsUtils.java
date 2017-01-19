/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.model.java.JpsJavaClasspathKind;
import org.jetbrains.jps.model.java.JpsJavaDependenciesEnumerator;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsLibraryRoot;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.util.JpsPathUtil;
import org.jetbrains.kotlin.config.TargetPlatformKind;
import org.jetbrains.kotlin.jps.JpsKotlinCompilerSettingsKt;
import org.jetbrains.kotlin.utils.LibraryUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class JpsUtils {
    private static final Map<ModuleBuildTarget, Boolean> IS_KOTLIN_JS_MODULE_CACHE = new ConcurrentHashMap<ModuleBuildTarget, Boolean>();
    private static final Map<String, Boolean> IS_KOTLIN_JS_STDLIB_JAR_CACHE = new ConcurrentHashMap<String, Boolean>();
    private JpsUtils() {}

    @NotNull
    static JpsJavaDependenciesEnumerator getAllDependencies(@NotNull ModuleBuildTarget target) {
        return JpsJavaExtensionService.dependencies(target.getModule()).recursively().exportedOnly()
                .includedIn(JpsJavaClasspathKind.compile(target.isTests()));
    }

    static boolean isJsKotlinModule(@NotNull ModuleBuildTarget target) {
        Boolean cachedValue = IS_KOTLIN_JS_MODULE_CACHE.get(target);
        if (cachedValue != null) return cachedValue;

        boolean isKotlinJsModule = isJsKotlinModuleImpl(target);
        IS_KOTLIN_JS_MODULE_CACHE.put(target, isKotlinJsModule);

        return isKotlinJsModule;
    }

    private static boolean isJsKotlinModuleImpl(@NotNull ModuleBuildTarget target) {
        TargetPlatformKind<?> targetPlatform = JpsKotlinCompilerSettingsKt.getTargetPlatform(target.getModule());
        if (targetPlatform != null) return targetPlatform == TargetPlatformKind.JavaScript.INSTANCE;

        Set<JpsLibrary> libraries = getAllDependencies(target).getLibraries();
        for (JpsLibrary library : libraries) {
            for (JpsLibraryRoot root : library.getRoots(JpsOrderRootType.COMPILED)) {
                String url = root.getUrl();

                Boolean cachedValue = IS_KOTLIN_JS_STDLIB_JAR_CACHE.get(url);
                if (cachedValue != null) {
                    if (cachedValue.booleanValue()) return true;
                    else continue;
                }

                boolean isKotlinJavascriptStdLibrary = LibraryUtils.isKotlinJavascriptStdLibrary(JpsPathUtil.urlToFile(url));
                IS_KOTLIN_JS_STDLIB_JAR_CACHE.put(url, isKotlinJavascriptStdLibrary);
                if (isKotlinJavascriptStdLibrary) return true;
            }
        }
        return false;
    }

    @TestOnly
    static void resetCaches() {
        IS_KOTLIN_JS_MODULE_CACHE.clear();
        IS_KOTLIN_JS_STDLIB_JAR_CACHE.clear();
    }
}
