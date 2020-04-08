/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle;

import com.intellij.openapi.externalSystem.importing.ImportSpec;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class TestImportSpecBuilder extends ImportSpecBuilder {
    public TestImportSpecBuilder(
            @NotNull Project project,
            @NotNull ProjectSystemId id
    ) {
        super(project, id);
    }

    public ImportSpecBuilder setCreateEmptyContentRoots(boolean value) {
        return createDirectoriesForEmptyContentRoots();
    }
}
