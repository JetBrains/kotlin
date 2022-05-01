/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import org.jetbrains.kotlin.statistics.metrics.StringAnonymizationPolicy.COMPONENT_VERSION
import kotlin.test.Test
import kotlin.test.assertEquals


class MetricPolicyTest {

    @Test
    fun versionAnonymization() {
        assertEquals("0.0.0", COMPONENT_VERSION.anonymize("some.invalid.string"))
        assertEquals("1.0.0", COMPONENT_VERSION.anonymize("1"))
        assertEquals("1.2.0", COMPONENT_VERSION.anonymize("1.2"))

        assertEquals("1.2.3", COMPONENT_VERSION.anonymize("1.2.3"))
        assertEquals("1.2.3", COMPONENT_VERSION.anonymize("1.2.3"))
        assertEquals("1.2.3", COMPONENT_VERSION.anonymize("1.2.3.4"))
        assertEquals("1.2.3-m", COMPONENT_VERSION.anonymize("1.2.3-M"))
        assertEquals("1.2.3-m1", COMPONENT_VERSION.anonymize("1.2.3-M1"))
        assertEquals("1.2.3-m2", COMPONENT_VERSION.anonymize("1.2.3.M2"))
        assertEquals("1.2.3-rc", COMPONENT_VERSION.anonymize("1.2.3-RC"))
        assertEquals("1.2.3-rc5", COMPONENT_VERSION.anonymize("1.2.3-RC5"))

        assertEquals("1.2.3", COMPONENT_VERSION.anonymize("1.2.3.unknown suffix"))
        assertEquals("1.2.3", COMPONENT_VERSION.anonymize("1.2.3-unknown suffix"))

        assertEquals("123.234.345-dev", COMPONENT_VERSION.anonymize("123.234.345-dev-12345"))
        assertEquals("1.7.255-snapshot", COMPONENT_VERSION.anonymize("1.7.255-SNAPSHOT"))

        assertEquals("1.7.255-beta", COMPONENT_VERSION.anonymize("1.7.255-beta"))
    }
}