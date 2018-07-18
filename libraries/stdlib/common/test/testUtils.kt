/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test

public expect fun assertTypeEquals(expected: Any?, actual: Any?)

internal expect fun String.removeLeadingPlusOnJava6(): String
