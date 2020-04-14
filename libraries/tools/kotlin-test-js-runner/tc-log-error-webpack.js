/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

'use strict';

import {formatMessage, TYPED_MESSAGE} from "./src/teamcity-format";

const ModuleNotFoundError = require("webpack/lib/ModuleNotFoundError")

class TeamCityErrorPlugin {
    apply(compiler) {
        compiler.hooks.done.tap('TeamCityErrorPlugin', (stats) => {
            stats.compilation.errors.forEach(error => {
                const type = 'error'
                if (error instanceof ModuleNotFoundError) {
                    error.dependencies.forEach(dependency => {
                        console[type](formatMessage(TYPED_MESSAGE, `Module '${dependency.request}' not found`, type))
                    })
                    return
                }

                console[type](formatMessage(TYPED_MESSAGE, error.message, type))
            })

            stats.compilation.warnings.forEach(warning => {
                const type = 'warn'

                console[type](formatMessage(TYPED_MESSAGE, warning.message, type))
            })
        });
    }
}

module.exports = TeamCityErrorPlugin;