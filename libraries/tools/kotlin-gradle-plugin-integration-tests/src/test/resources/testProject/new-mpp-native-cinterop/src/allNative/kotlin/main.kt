/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import example.cinterop.project.*
import example.cinterop.published.*

fun dependentProject() {
    projectPrint("Dependent: Project print")
    publishedPrint("Dependent: Published print")
}
