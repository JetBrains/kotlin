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

package org.jetbrains.kotlin.idea.stubindex;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtNamedDeclaration;

import java.util.Collection;

public class KotlinPropertyShortNameIndex extends StringStubIndexExtension<KtNamedDeclaration> {
    private static final StubIndexKey<String, KtNamedDeclaration> KEY = KotlinIndexUtil.createIndexKey(KotlinPropertyShortNameIndex.class);

    private static final KotlinPropertyShortNameIndex ourInstance = new KotlinPropertyShortNameIndex();

    public static KotlinPropertyShortNameIndex getInstance() {
        return ourInstance;
    }

    private KotlinPropertyShortNameIndex() {}

    @NotNull
    @Override
    public StubIndexKey<String, KtNamedDeclaration> getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public Collection<KtNamedDeclaration> get(@NotNull String s, @NotNull Project project, @NotNull GlobalSearchScope scope) {
        return StubIndex.getElements(KEY, s, project, scope, KtNamedDeclaration.class);
    }
}
