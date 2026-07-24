/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.example.dagger.kotlin

import com.example.dagger.kotlin.ui.HomeActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component
interface TestComponent {
    fun inject(application: BaseApplication)
}
