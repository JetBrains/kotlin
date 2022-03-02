/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.jps.model.serialization;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * This API exists starting from 221 IDEA
 *
 * Copy-pasted from https://github.com/JetBrains/intellij-community/blob/master/jps/model-serialization/src/org/jetbrains/jps/model/serialization/JpsPathMacroContributor.java
 */
public interface JpsPathMacroContributor {
    @NotNull
    Map<String, String> getPathMacros();
}
