/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.compiler.ant.taskdefs;

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.GenerationOptions;
import com.intellij.compiler.ant.ModuleChunk;
import com.intellij.compiler.ant.Tag;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 */
public class Javac extends Tag {

    public Javac(GenerationOptions genOptions, ModuleChunk moduleChunk, final String outputDir) {
        super(getTagName(genOptions, moduleChunk), getAttributes(genOptions, outputDir, moduleChunk));
    }

    private static String getTagName(GenerationOptions genOptions, ModuleChunk moduleChunk) {
        if (moduleChunk.getCustomCompilers().length > 0) {
            return "instrumentIdeaExtensions";
        }
        return genOptions.enableFormCompiler ? "javac2" : "javac";
    }

    private static Pair[] getAttributes(GenerationOptions genOptions, String outputDir, ModuleChunk moduleChunk) {
        final List<Pair> pairs = new ArrayList<>();
        pairs.add(pair("destdir", outputDir));
        if (moduleChunk.getCustomCompilers().length == 0) {
            pairs.add(pair("debug", BuildProperties.propertyRef(BuildProperties.PROPERTY_COMPILER_GENERATE_DEBUG_INFO)));
            pairs.add(pair("nowarn", BuildProperties.propertyRef(BuildProperties.PROPERTY_COMPILER_GENERATE_NO_WARNINGS)));
            pairs.add(pair("memorymaximumsize", BuildProperties.propertyRef(BuildProperties.PROPERTY_COMPILER_MAX_MEMORY)));
            pairs.add(pair("fork", "true"));
            if (genOptions.forceTargetJdk) {
                pairs.add(pair("executable", getExecutable(moduleChunk.getName())));
            }
        }
        return pairs.toArray(new Pair[0]);
    }

    @Nullable
    @NonNls
    private static String getExecutable(String moduleName) {
        if (moduleName == null) {
            return null;
        }
        return BuildProperties.propertyRef(BuildProperties.getModuleChunkJdkBinProperty(moduleName)) + "/javac";
    }
}
