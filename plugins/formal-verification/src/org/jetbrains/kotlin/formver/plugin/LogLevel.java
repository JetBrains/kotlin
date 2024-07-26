/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin;

import org.jetbrains.annotations.NotNull;

public enum LogLevel {
    ONLY_WARNINGS, FULL_VIPER_DUMP;

    @NotNull
    public static LogLevel defaultLogLevel() {
        return ONLY_WARNINGS;
    }
}
