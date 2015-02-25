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

import java.io.File;
import java.util.Collection;

public class SimpleOutputItem {
    private final Collection<File> sourceFiles;
    private final File outputFile;

    public SimpleOutputItem(@NotNull Collection<File> sourceFiles, @NotNull File outputFile) {
        this.sourceFiles = sourceFiles;
        this.outputFile = outputFile;
    }

    @NotNull
    public Collection<File> getSourceFiles() {
        return sourceFiles;
    }

    @NotNull
    public File getOutputFile() {
        return outputFile;
    }

    @Override
    public String toString() {
        return sourceFiles + " -> " + outputFile;
    }
}
