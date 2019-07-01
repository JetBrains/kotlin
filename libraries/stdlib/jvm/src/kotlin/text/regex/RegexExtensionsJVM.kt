/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")

package kotlin.text


/**
 * Converts this [java.util.regex.Pattern] to an instance of [Regex].
 *
 * Provides the way to use Regex API on the instances of [java.util.regex.Pattern].
 */
@kotlin.internal.InlineOnly
public inline fun java.util.regex.Pattern.toRegex(): Regex = Regex(this)