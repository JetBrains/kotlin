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

package org.jetbrains.kotlin.compilerRunner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.preloading.ClassPreloadingUtils;
import org.jetbrains.kotlin.preloading.Preloader;
import org.jetbrains.kotlin.utils.KotlinPaths;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation.NO_LOCATION;
import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR;

public class CompilerRunnerUtil {

    private static SoftReference<ClassLoader> ourClassLoaderRef = new SoftReference<ClassLoader>(null);

    @NotNull
    private static synchronized ClassLoader getOrCreateClassLoader(
            @NotNull JpsCompilerEnvironment environment,
            @NotNull File libPath
    ) throws IOException {
        ClassLoader classLoader = ourClassLoaderRef.get();
        if (classLoader == null) {
            classLoader = ClassPreloadingUtils.preloadClasses(
                    Collections.singletonList(new File(libPath, "kotlin-compiler.jar")),
                    Preloader.DEFAULT_CLASS_NUMBER_ESTIMATE,
                    CompilerRunnerUtil.class.getClassLoader(),
                    environment.getClassesToLoadByParent()
            );
            ourClassLoaderRef = new SoftReference<ClassLoader>(classLoader);
        }
        return classLoader;
    }

    @Nullable
    public static File getLibPath(@NotNull KotlinPaths paths, @NotNull MessageCollector messageCollector) {
        File libs = paths.getLibPath();
        if (libs.exists() && !libs.isFile()) return libs;

        messageCollector.report(
                ERROR,
                "Broken compiler at '" + libs.getAbsolutePath() + "'. Make sure plugin is properly installed",
                NO_LOCATION
        );

        return null;
    }

    @Nullable
    public static Object invokeExecMethod(
            @NotNull String compilerClassName,
            @NotNull String[] arguments,
            @NotNull JpsCompilerEnvironment environment,
            @NotNull MessageCollector messageCollector,
            @NotNull PrintStream out
    ) throws Exception {
        File libPath = getLibPath(environment.getKotlinPaths(), messageCollector);
        if (libPath == null) return null;

        ClassLoader classLoader = getOrCreateClassLoader(environment, libPath);

        Class<?> kompiler = Class.forName(compilerClassName, true, classLoader);
        Method exec = kompiler.getMethod(
                "execAndOutputXml",
                PrintStream.class,
                Class.forName("org.jetbrains.kotlin.config.Services", true, classLoader),
                String[].class
        );

        return exec.invoke(kompiler.newInstance(), out, environment.getServices(), arguments);
    }
}
