/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal.unsafe

private fun monitorEnter(@Suppress("UNUSED_PARAMETER") monitor: Any): Unit =
    throw UnsupportedOperationException("This function can only be used privately")

private fun monitorExit(@Suppress("UNUSED_PARAMETER") monitor: Any): Unit =
    throw UnsupportedOperationException("This function can only be used privately")
