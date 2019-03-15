/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package example.cinterop.project

import example.cinterop.project.stdio.*

fun projectPrint(str: String) {
    printf(str + '\n')
}
