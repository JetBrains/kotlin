/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.test;

import kotlin.Metadata;
import kotlin.metadata.KmClass;
import kotlin.metadata.jvm.JvmMetadataVersion;
import kotlin.metadata.jvm.KotlinClassHeader;
import kotlin.metadata.jvm.KotlinClassMetadata;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.*;

public class JavaUsageTest {

    @Test
    public void testKotlinClassHeader() {
        Metadata m = MetadataSmokeTest.class.getAnnotation(Metadata.class);
        KmClass clazz1 = ((KotlinClassMetadata.Class) Objects.requireNonNull(KotlinClassMetadata.readStrict(m))).getKmClass();
        KotlinClassHeader kh = new KotlinClassHeader(m.k(), m.mv(), m.d1(), m.d2(), m.xs(), m.pn(), m.xi());
        KmClass clazz2 = ((KotlinClassMetadata.Class) Objects.requireNonNull(KotlinClassMetadata.readStrict(kh))).getKmClass();
        assertEquals(clazz1.getName(), clazz2.getName());
    }

    @Test
    public void testWritingBackWithDefaults() {
        Metadata m = MetadataSmokeTest.class.getAnnotation(Metadata.class);
        KotlinClassMetadata clazz1 = ((KotlinClassMetadata) Objects.requireNonNull(KotlinClassMetadata.readStrict(m)));
        Metadata written = clazz1.write();
        //noinspection KotlinInternalInJava
        assertArrayEquals(written.mv(), JvmMetadataVersion.LATEST_STABLE_SUPPORTED.toIntArray());
        assertEquals(50, written.xi());
    }
}
