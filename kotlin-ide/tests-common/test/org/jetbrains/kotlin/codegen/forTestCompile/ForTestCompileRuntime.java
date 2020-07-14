/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.forTestCompile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class ForTestCompileRuntime {
    private static volatile SoftReference<ClassLoader> runtimeJarClassLoader = new SoftReference<>(null);

    @NotNull
    public static File coroutinesCompatForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-coroutines-experimental-compat.jar"));
    }

    @NotNull
    private static File assertExists(@NotNull File file) {
        File absolute = new File(KotlinTestUtils.getHomeDirectory() + "/..", file.getPath());
        if (!absolute.exists()) {
            throw new IllegalStateException(absolute + " does not exist. Run 'gradlew dist'");
        }
        return absolute;
    }

    @NotNull
    public static synchronized ClassLoader runtimeJarClassLoader() {
        ClassLoader loader = runtimeJarClassLoader.get();
        if (loader == null) {
            KotlinArtifacts artifacts = KotlinArtifacts.getInstance();
            loader = createClassLoader(artifacts.getKotlinStdlib(), artifacts.getKotlinScriptRuntime(), artifacts.getKotlinTest());
            runtimeJarClassLoader = new SoftReference<>(loader);
        }
        return loader;
    }

    @NotNull
    private static ClassLoader createClassLoader(@NotNull File... files) {
        try {
            List<URL> urls = new ArrayList<>(2);
            for (File file : files) {
                urls.add(file.toURI().toURL());
            }
            return new URLClassLoader(urls.toArray(new URL[urls.size()]), null);
        }
        catch (MalformedURLException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }
}
