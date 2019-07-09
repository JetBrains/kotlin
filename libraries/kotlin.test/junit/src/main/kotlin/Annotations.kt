/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmPackageName("kotlin.test.junit.annotations")

package kotlin.test

public actual typealias Test = org.junit.Test
public actual typealias Ignore = org.junit.Ignore
public actual typealias BeforeTest = org.junit.Before
public actual typealias AfterTest = org.junit.After
