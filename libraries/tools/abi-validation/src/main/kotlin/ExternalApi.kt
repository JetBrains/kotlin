/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

/**
 * API that is used externally and programmatically by binary-compatibility-validator tool in Kotlin standard library
 */
@Retention(AnnotationRetention.SOURCE)
annotation class ExternalApi
