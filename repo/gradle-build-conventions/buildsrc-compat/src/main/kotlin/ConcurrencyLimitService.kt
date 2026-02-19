import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class ConcurrencyLimitService : BuildService<ConcurrencyLimitService.Params> {
    interface Params : BuildServiceParameters

}