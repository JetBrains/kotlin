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

import java.util.Arrays;
import java.util.Objects;

import static org.junit.Assert.*;

public class JavaUsageTest {

    @Test
    public void testKotlinClassHeader() {
        Metadata m = MetadataSmokeTest.class.getAnnotation(Metadata.class);
        KmClass clazz1 = ((KotlinClassMetadata.Class) Objects.requireNonNull(KotlinClassMetadata.read(m))).getKmClass();
        KotlinClassHeader kh = new KotlinClassHeader(m.k(), m.mv(), m.d1(), m.d2(), m.xs(), m.pn(), m.xi());
        KmClass clazz2 = ((KotlinClassMetadata.Class) Objects.requireNonNull(KotlinClassMetadata.read(kh))).getKmClass();
        assertEquals(clazz1.getName(), clazz2.getName());
    }

    @Test
    public void testWritingBackWithDefaults() {
        Metadata m = MetadataSmokeTest.class.getAnnotation(Metadata.class);
        KmClass clazz1 = ((KotlinClassMetadata.Class) Objects.requireNonNull(KotlinClassMetadata.read(m))).getKmClass();
        Metadata written = KotlinClassMetadata.writeClass(clazz1);
        assertArrayEquals(written.mv(), KotlinClassMetadata.COMPATIBLE_METADATA_VERSION);
        assertEquals(0, written.xi());
    }
}
