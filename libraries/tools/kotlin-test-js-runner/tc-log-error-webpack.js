/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

'use strict';

import {formatMessage, TYPED_MESSAGE} from "./src/teamcity-format";

const ModuleNotFoundError = require("webpack/lib/ModuleNotFoundError")

class TeamCityErrorPlugin {
    warningsFilter(value) {
        if (!Array.isArray(value)) {
            value = value ? [value] : [];
        }
        return value.map(filter => {
            if (typeof filter === "string") {
                return (warning, warningString) => warningString.includes(filter);
            }
            if (filter instanceof RegExp) {
                return (warning, warningString) => filter.test(warningString);
            }
            if (typeof filter === "function") {
                return filter;
            }
            throw new Error(
                `Can only filter warnings with Strings or RegExps. (Given: ${filter})`
            );
        });
    }

    apply(compiler) {
        compiler.hooks.done.tap('TeamCityErrorPlugin', (stats) => {

            const compilation = stats.compilation;
            const warningsFilters = this.warningsFilter(compilation.options.ignoreWarnings);

            compilation.errors.forEach(error => {
                const type = 'error';
                if (error instanceof ModuleNotFoundError) {
                    console[type](
                        formatMessage(TYPED_MESSAGE, error.message, type)
                    );
                } else {
                    console[type](formatMessage(TYPED_MESSAGE, error.message, type));
                }
            });

            compilation.warnings
                .filter(warning => warningsFilters.every(warningFilter => !warningFilter(warning, compilation)))
                .forEach(warning => {
                    const type = 'warn';

                    console[type](formatMessage(TYPED_MESSAGE, warning.message, type));
                });
        });
    }
}

module.exports = TeamCityErrorPlugin;