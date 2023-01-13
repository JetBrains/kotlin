/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.test;

import kotlin.Metadata;
import kotlinx.metadata.KmClass;
import kotlinx.metadata.jvm.KotlinClassHeader;
import kotlinx.metadata.jvm.KotlinClassMetadata;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JavaUsageTest {
    @SuppressWarnings("ConstantConditions")
    @Test
    public void testKotlinClassHeader() {
        Metadata m = MetadataSmokeTest.class.getAnnotation(Metadata.class);
        KmClass clazz1 = ((KotlinClassMetadata.Class) KotlinClassMetadata.read(m)).toKmClass();
        KotlinClassHeader kh = new KotlinClassHeader(m.k(), m.mv(),m.d1(),m.d2(), m.xs(), m.pn(), m.xi());
        KmClass clazz2 = ((KotlinClassMetadata.Class) KotlinClassMetadata.read(kh)).toKmClass();
        assertEquals(clazz1.getName(), clazz2.getName());
    }
}
