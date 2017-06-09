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

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SourceFilePathResolver {
    @NotNull
    private final Set<File> sourceRoots;

    @NotNull
    private final Map<File, String> cache = new HashMap<>();

    public SourceFilePathResolver(@NotNull List<File> sourceRoots) {
        this.sourceRoots = new HashSet<>();
        for (File sourceRoot : sourceRoots) {
            this.sourceRoots.add(sourceRoot.getAbsoluteFile());
        }
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

    private String calculatePathRelativeToSourceRoots(@NotNull File file) throws IOException {
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
}
