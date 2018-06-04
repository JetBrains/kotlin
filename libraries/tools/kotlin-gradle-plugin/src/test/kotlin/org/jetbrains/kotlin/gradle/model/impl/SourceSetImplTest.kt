/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.impl

import nl.jqno.equalsverifier.EqualsVerifier
import nl.jqno.equalsverifier.Warning
import org.junit.Test

class SourceSetImplTest {
    @Test
    @Throws(Exception::class)
    fun equals() {
        EqualsVerifier.forClass(SourceSetImpl::class.java).suppress(Warning.NULL_FIELDS).verify()
    }
}