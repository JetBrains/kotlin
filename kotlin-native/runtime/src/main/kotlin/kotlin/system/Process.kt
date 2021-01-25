/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.system

import kotlin.native.internal.GCCritical

/**
 * Terminates the currently running process.
 *
 * @param status serves as a status code; by convention,
 * a nonzero status code indicates abnormal termination.
 *
 * @return This method never returns normally.
 */
@SymbolName("Kotlin_system_exitProcess")
@GCCritical
public external fun exitProcess(status: Int): Nothing