/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.utils

import org.jetbrains.kotlin.name.FqName

object LombokNames {

    val ACCESSORS = FqName("lombok.experimental.Accessors")
    val GETTER = FqName("lombok.Getter")
    val SETTER = FqName("lombok.Setter")
    val WITH = FqName("lombok.With")
    val VALUE = FqName("lombok.lombok.Value")
    val NO_ARGS_CONSTRUCTOR = FqName("lombok.NoArgsConstructor")
    val ALL_ARGS_CONSTRUCTOR = FqName("lombok.AllArgsConstructor")
    val REQUIRED_ARGS_CONSTRUCTOR = FqName("lombok.RequiredArgsConstructor")


    //taken from idea lombok plugin
    val NON_NULL_ANNOTATIONS = listOf(
        "androidx.annotation.NonNull",
        "android.support.annotation.NonNull",
        "com.sun.istack.internal.NotNull",
        "edu.umd.cs.findbugs.annotations.NonNull",
        "javax.annotation.Nonnull",
        "lombok.NonNull",
        "org.checkerframework.checker.nullness.qual.NonNull",
        "org.eclipse.jdt.annotation.NonNull",
        "org.eclipse.jgit.annotations.NonNull",
        "org.jetbrains.annotations.NotNull",
        "org.jmlspecs.annotation.NonNull",
        "org.netbeans.api.annotations.common.NonNull",
        "org.springframework.lang.NonNull"
    ).map { FqName(it) }.toSet()

}
