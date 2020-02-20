// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import org.jetbrains.annotations.ApiStatus;

/**
 * Provides "read-only" facade api for IDE project model.
 *
 * TODO add methods to access project model elements (likely in terms of new workspace model) which can be resolved by Gradle import
 */
@ApiStatus.Experimental
public interface GradleProjectModel {
}
