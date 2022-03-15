/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import {KotlinTestRunner} from "./KotlinTestRunner";

export class EmptyKotlinTestRunner implements KotlinTestRunner {
    suite(name: string, isIgnored: boolean, fn: () => void): void {
    }

    test(name: string, isIgnored: boolean, fn: () => void): void {
    }

}