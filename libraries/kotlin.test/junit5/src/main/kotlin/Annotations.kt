/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */


@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmPackageName("kotlin.test.junit5.annotations")
package kotlin.test

public actual typealias Test = org.junit.jupiter.api.Test
public actual typealias Ignore = org.junit.jupiter.api.Disabled
public actual typealias BeforeTest = org.junit.jupiter.api.BeforeEach
public actual typealias AfterTest = org.junit.jupiter.api.AfterEach
