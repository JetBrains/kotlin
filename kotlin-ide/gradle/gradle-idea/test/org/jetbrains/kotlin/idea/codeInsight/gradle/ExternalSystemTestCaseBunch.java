/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle;

import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.task.ProjectTaskManager;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.concurrency.Promise;
import java.util.Arrays;

//FIX ME WHEN BUNCH 192 REMOVED
class ExternalSystemTestCaseBunch {

    protected static boolean isDefaultRefreshCallback(Object callback) {
        return callback instanceof ImportSpecBuilder.DefaultProjectRefreshCallback;
    }

    protected static void build(Object[] buildableElements, Project myProject) {
        Promise<ProjectTaskManager.Result> promise;
        if (buildableElements instanceof Module[]) {
            promise = ProjectTaskManager.getInstance(myProject).build((Module[])buildableElements);
        }
        else if (buildableElements instanceof Artifact[]) {
            promise = ProjectTaskManager.getInstance(myProject).build((Artifact[])buildableElements);
        }
        else {
            throw new AssertionError("Unsupported buildableElements: " + Arrays.toString(buildableElements));
        }
        EdtTestUtil.runInEdtAndWait(() -> PlatformTestUtil.waitForPromise(promise));
    }

}
