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

package org.jetbrains.kotlin.idea.stubindex;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtTypeAlias;

import java.util.Collection;

public class KotlinTypeAliasShortNameIndex extends StringStubIndexExtension<KtTypeAlias> {
    private static final StubIndexKey<String, KtTypeAlias> KEY = KotlinIndexUtil.createIndexKey(KotlinTypeAliasShortNameIndex.class);

    private static final KotlinTypeAliasShortNameIndex ourInstance = new KotlinTypeAliasShortNameIndex();

    public static KotlinTypeAliasShortNameIndex getInstance() {
        return ourInstance;
    }

    private KotlinTypeAliasShortNameIndex() {}

    @NotNull
    @Override
    public StubIndexKey<String, KtTypeAlias> getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public Collection<KtTypeAlias> get(@NotNull String s, @NotNull Project project, @NotNull GlobalSearchScope scope) {
        return StubIndex.getElements(KEY, s, project, scope, KtTypeAlias.class);
    }
}
