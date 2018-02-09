/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */


@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmPackageName("kotlin.test.annotations")
package kotlin.test

public actual typealias Test = org.testng.annotations.Test
public actual typealias Ignore = org.testng.annotations.Ignore
public actual typealias BeforeTest = org.testng.annotations.BeforeClass
public actual typealias AfterTest = org.testng.annotations.AfterClass
