/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.jvm;

import kotlin.DeprecationLevel;
import kotlin.Metadata;
import kotlin.ReplaceWith;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion;

import java.lang.annotation.Annotation;
import java.util.Arrays;


/**
 * An implementation of the {@link Metadata} annotation on a JVM class file, containing the metadata of Kotlin declarations declared in the class file.
 * This class is intended to be used by Java clients to be able to pass a {@link Metadata} instance to various methods in the library if it cannot be obtained via reflection.
 * Kotlin clients should be able to instantiate annotation directly and therefore should not use this class.
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
@kotlin.Deprecated(
        message = "Kotlin clients should instantiate Metadata annotation directly",
        replaceWith = @ReplaceWith(expression = "Metadata", imports = {}),
        level = DeprecationLevel.ERROR
)
public final class KotlinClassHeader implements Metadata {
    private final int k;
    @NotNull private final int[] mv;
    @NotNull private final String[] d1;
    @NotNull private final String[] d2;
    @NotNull private final String xs;
    @NotNull private final String pn;
    private final int xi;

    /**
     * @param kind            see {@link Metadata#k()}
     * @param metadataVersion see {@link Metadata#mv()}
     * @param data1           see {@link Metadata#d1()}
     * @param data2           see {@link Metadata#d2()}
     * @param extraString     see {@link Metadata#xs()}
     * @param packageName     see {@link Metadata#pn()}
     * @param extraInt        see {@link Metadata#xi()}
     */
    @SuppressWarnings("SSBasedInspection")
    @kotlin.Deprecated(
            message = "Kotlin clients should instantiate Metadata annotation directly",
            replaceWith = @ReplaceWith(
                    expression = "kotlinx.metadata.jvm.Metadata(kind, metadataVersion, data1, data2, extraString, packageName, extraInt)",
                    imports = {}
            ),
            level = DeprecationLevel.ERROR
    )
    public KotlinClassHeader(
            @Nullable Integer kind,
            @Nullable int[] metadataVersion,
            @Nullable String[] data1,
            @Nullable String[] data2,
            @Nullable String extraString,
            @Nullable String packageName,
            @Nullable Integer extraInt
    ) {
        if (kind != null) this.k = kind; else this.k = 1;
        if (metadataVersion != null) this.mv = metadataVersion; else this.mv = new int[0];
        if (data1 != null) this.d1 = data1; else this.d1 = new String[0];
        if (data2 != null) this.d2 = data2; else this.d2 = new String[0];
        if (extraString != null) this.xs = extraString; else this.xs = "";
        if (packageName != null) this.pn = packageName; else this.pn = "";
        if (extraInt != null) this.xi = extraInt; else this.xi = 0;
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public int k() {
        return k;
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public int[] mv() {
        return mv;
    }

    /**
     * {@inheritdoc}
     */
    @Override
    @Deprecated
    public int[] bv() {
        return new int[] {1, 0, 3};
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public String[] d1() {
        return d1;
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public String[] d2() {
        return d2;
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public String xs() {
        return xs;
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public String pn() {
        return pn;
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public int xi() {
        return xi;
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public Class<? extends Annotation> annotationType() {
        return Metadata.class;
    }

    // TODO: equals, hashCode, etc as in Java Annotation contract?

    /**
     * Use {@link KotlinClassMetadata#CLASS_KIND} instead
     */
    @Deprecated
    public static final int CLASS_KIND = 1;

    /**
     * Use {@link KotlinClassMetadata#FILE_FACADE_KIND} instead
     */
    @Deprecated
    public static final int FILE_FACADE_KIND = 2;

    /**
     * Use {@link KotlinClassMetadata#SYNTHETIC_CLASS_KIND} instead
     */
    @Deprecated
    public static final int SYNTHETIC_CLASS_KIND = 3;

    /**
     * Use {@link KotlinClassMetadata#MULTI_FILE_CLASS_FACADE_KIND} instead
     */
    @Deprecated
    public static final int MULTI_FILE_CLASS_FACADE_KIND = 4;

    /**
     * Use {@link KotlinClassMetadata#MULTI_FILE_CLASS_PART_KIND} instead
     */
    @Deprecated
    public static final int MULTI_FILE_CLASS_PART_KIND = 5;

    /**
     * Use {@link kotlinx.metadata.jvm.JvmMetadataVersion#LATEST_STABLE_SUPPORTED} instead
     */
    @Deprecated
    public static final int[] COMPATIBLE_METADATA_VERSION = Arrays.copyOf(JvmMetadataVersion.INSTANCE.toArray(), 3);
}
