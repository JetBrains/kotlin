/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.jvm.internal

/**
 * Workaround for https://github.com/Kotlin/binary-compatibility-validator/issues/104:
 *
 * When internal declaration has some classes that were relocated by shadowJar plugin,
 * binary-compatibility-validator can't filter them out.
 * Therefore, we explicitly exclude declarations marked with this annotation.
 */
@Retention(AnnotationRetention.BINARY)
internal annotation class IgnoreInApiDump
