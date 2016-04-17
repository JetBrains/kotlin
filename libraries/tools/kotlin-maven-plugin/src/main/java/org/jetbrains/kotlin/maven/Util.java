/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.maven;

import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;

import java.io.File;
import java.util.List;

public class Util {
    static List<String> filterClassPath(final File basedir, List<String> classpath) {
        return CollectionsKt.filter(classpath, new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String s) {
                return new File(s).exists() || new File(basedir, s).exists();
            }
        });
    }
}
