/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.sourceMap;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.config.JSConfigurationKeys;
import org.jetbrains.kotlin.js.config.JsConfig;
import org.jetbrains.kotlin.js.inline.util.RelativePathCalculator;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SourceFilePathResolver {
    @NotNull
    private final Set<File> sourceRoots;

    @Nullable
    private final RelativePathCalculator outputDirPathResolver;

    @NotNull
    private final Map<File, String> cache = new HashMap<>();

    public SourceFilePathResolver(@NotNull List<File> sourceRoots, @Nullable File outputDir) {
        this.sourceRoots = new HashSet<>();
        for (File sourceRoot : sourceRoots) {
            this.sourceRoots.add(sourceRoot.getAbsoluteFile());
        }

        outputDirPathResolver = outputDir != null ? new RelativePathCalculator(outputDir) : null;
    }

    @NotNull
    public String getPathRelativeToSourceRoots(@NotNull File file) throws IOException {
        String path = cache.get(file);
        if (path == null) {
            path = calculatePathRelativeToSourceRoots(file);
            cache.put(file, path);
        }
        return path;
    }

    @NotNull
    private String calculatePathRelativeToSourceRoots(@NotNull File file) throws IOException {
        String pathRelativeToOutput = calculatePathRelativeToOutput(file);
        if (pathRelativeToOutput != null) return pathRelativeToOutput;

        List<String> parts = new ArrayList<>();
        File currentFile = file.getCanonicalFile();

        while (currentFile != null) {
            if (sourceRoots.contains(currentFile)) {
                if (parts.isEmpty()) {
                    break;
                }
                Collections.reverse(parts);
                return StringUtil.join(parts, File.separator);
            }
            parts.add(currentFile.getName());
            currentFile = currentFile.getParentFile();
        }
        return file.getName();
    }

    @Nullable
    private String calculatePathRelativeToOutput(@NotNull File file) {
        return outputDirPathResolver != null ? outputDirPathResolver.calculateRelativePathTo(file) : null;
    }

    @NotNull
    public static SourceFilePathResolver create(@NotNull JsConfig config) {
        List<File> sourceRoots = config.getSourceMapRoots().stream().map(File::new).collect(Collectors.toList());
        File outputDir = null;
        if (config.shouldGenerateRelativePathsInSourceMap()) {
            outputDir = config.getConfiguration().get(JSConfigurationKeys.OUTPUT_DIR);
        }
        return new SourceFilePathResolver(sourceRoots, outputDir);
    }
}
