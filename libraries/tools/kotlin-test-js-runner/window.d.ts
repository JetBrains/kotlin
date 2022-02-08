/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import {KotlinTestRunner} from "./src/KotlinTestRunner";

declare global {
    interface Window {
        __karma__: {
            config: {
                args: string[]
            },
            result: (result: BrowserResult) => void
        }

        kotlinTest: {
            adapterTransformer: (current: KotlinTestRunner) => KotlinTestRunner
        }
    }
}

interface BrowserResult {
}