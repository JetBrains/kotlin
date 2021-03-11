/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.serialization.JpsModelSerializationDataService
import org.jetbrains.kotlin.incremental.storage.RelativeFileToPathConverter

internal class JpsFileToPathConverter(
    jpsProject: JpsProject
) : RelativeFileToPathConverter(JpsModelSerializationDataService.getBaseDirectory(jpsProject))
