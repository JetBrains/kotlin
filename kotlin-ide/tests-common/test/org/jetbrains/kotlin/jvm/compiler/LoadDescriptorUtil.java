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

package org.jetbrains.kotlin.jvm.compiler;

import com.intellij.openapi.util.io.FileUtil;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.output.OutputUtilsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.codegen.GenerationUtils;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class LoadDescriptorUtil {
    private LoadDescriptorUtil() {
    }

    @NotNull
    public static ModuleDescriptor compileKotlinToDirAndGetModule(
            @NotNull List<File> kotlinFiles, @NotNull File outDir, @NotNull KotlinCoreEnvironment environment
    ) {
        GenerationState state = GenerationUtils.compileFiles(createKtFiles(kotlinFiles, environment), environment);
        OutputUtilsKt.writeAllTo(state.getFactory(), outDir);
        return state.getModule();
    }

    @NotNull
    private static List<KtFile> createKtFiles(@NotNull List<File> kotlinFiles, @NotNull KotlinCoreEnvironment environment) {
        return CollectionsKt.map(kotlinFiles, kotlinFile -> {
            try {
                return KotlinTestUtils.createFile(kotlinFile.getName(), FileUtil.loadFile(kotlinFile, true), environment.getProject());
            }
            catch (IOException e) {
                throw ExceptionUtilsKt.rethrow(e);
            }
        });
    }
}
