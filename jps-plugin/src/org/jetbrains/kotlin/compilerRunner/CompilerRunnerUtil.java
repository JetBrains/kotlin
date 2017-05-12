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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR;

public class CompilerRunnerUtil {

    private static SoftReference<ClassLoader> ourClassLoaderRef = new SoftReference<>(null);

    @NotNull
    private static synchronized ClassLoader getOrCreateClassLoader(
            @NotNull JpsCompilerEnvironment environment,
            @NotNull List<File> paths
    ) throws IOException {
        ClassLoader classLoader = ourClassLoaderRef.get();
        if (classLoader == null) {
            classLoader = ClassPreloadingUtils.preloadClasses(
                    paths,
                    Preloader.DEFAULT_CLASS_NUMBER_ESTIMATE,
                    CompilerRunnerUtil.class.getClassLoader(),
                    environment.getClassesToLoadByParent()
            );
            ourClassLoaderRef = new SoftReference<>(classLoader);
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
                null
        );

        return null;
    }

    @Nullable
    public static Object invokeExecMethod(
            @NotNull String compilerClassName,
            @NotNull String[] arguments,
            @NotNull JpsCompilerEnvironment environment,
            @NotNull PrintStream out
    ) throws Exception {
        File libPath = getLibPath(environment.getKotlinPaths(), environment.getMessageCollector());
        if (libPath == null) return null;

        List<File> paths = new ArrayList<>();
        paths.add(new File(libPath, "kotlin-compiler.jar"));

        if (Arrays.asList(arguments).contains("-Xuse-javac")) {
            File toolsJar = getJdkToolsJar();
            if (toolsJar != null) {
                paths.add(toolsJar);
            }
        }

        ClassLoader classLoader = getOrCreateClassLoader(environment, paths);

        Class<?> kompiler = Class.forName(compilerClassName, true, classLoader);
        Method exec = kompiler.getMethod(
                "execAndOutputXml",
                PrintStream.class,
                Class.forName("org.jetbrains.kotlin.config.Services", true, classLoader),
                String[].class
        );

        return exec.invoke(kompiler.newInstance(), out, environment.getServices(), arguments);
    }

    @Nullable
    static File getJdkToolsJar() throws IOException {
        String javaHomePath = System.getProperty("java.home");
        if (javaHomePath == null || javaHomePath.isEmpty()) {
            return null;
        }
        File javaHome = new File(javaHomePath);
        File toolsJar = new File(javaHome, "lib/tools.jar");
        if (toolsJar.exists()) {
            return toolsJar.getCanonicalFile();
        }

        // We might be inside jre.
        if (javaHome.getName().equals("jre")) {
            toolsJar = new File(javaHome.getParent(), "lib/tools.jar");
            if (toolsJar.exists()) {
                return toolsJar.getCanonicalFile();
            }
        }

        return null;
    }

}
