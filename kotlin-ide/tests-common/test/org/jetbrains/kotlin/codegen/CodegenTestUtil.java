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

package org.jetbrains.kotlin.codegen;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.util.io.FileUtil;
import kotlin.collections.CollectionsKt;
import kotlin.io.FilesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.idea.artifacts.TestKotlinArtifacts;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.StringsKt;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class CodegenTestUtil {
    private CodegenTestUtil() {
    }

    public static Method findDeclaredMethodByNameOrNull(@NotNull Class<?> aClass, @NotNull String name) {
        for (Method method : aClass.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    public static void compileJava(
            @NotNull List<String> fileNames,
            @NotNull List<String> additionalClasspath,
            @NotNull List<String> additionalOptions,
            @NotNull File outDirectory
    ) {
        try {
            List<String> classpath = new ArrayList<>();
            classpath.add(TestKotlinArtifacts.INSTANCE.getKotlinStdlib().getPath());
            classpath.add(TestKotlinArtifacts.INSTANCE.getKotlinReflect().getPath());
            classpath.add(TestKotlinArtifacts.INSTANCE.getJetbrainsAnnotations().getPath());
            classpath.addAll(additionalClasspath);

            List<String> options = new ArrayList<>(Arrays.asList(
                    "-classpath", StringsKt.join(classpath, File.pathSeparator),
                    "-d", outDirectory.getPath()
            ));
            options.addAll(additionalOptions);

            KotlinTestUtils.compileJavaFiles(CollectionsKt.map(fileNames, File::new), options);
        }
        catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }
}
