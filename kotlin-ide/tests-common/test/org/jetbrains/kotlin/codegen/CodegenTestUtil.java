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

import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.StringsKt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CodegenTestUtil {
    private CodegenTestUtil() {
    }

    public static void compileJava(
            @NotNull List<String> fileNames,
            @NotNull List<String> additionalClasspath,
            @NotNull List<String> additionalOptions,
            @NotNull File outDirectory
    ) {
        try {
            List<String> classpath = new ArrayList<>();
            classpath.add(KotlinArtifacts.getInstance().getKotlinStdlib().getPath());
            classpath.add(KotlinArtifacts.getInstance().getKotlinReflect().getPath());
            classpath.add(KotlinArtifacts.getInstance().getJetbrainsAnnotations().getPath());
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
