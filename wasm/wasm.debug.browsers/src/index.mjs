/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import formatters from "./formatters/index.mjs";

window.devtoolsFormatters = Array.isArray(window.devtoolsFormatters) ? window.devtoolsFormatters : []
window.devtoolsFormatters.push({
    header(value, configObject) {
        const formatter = formatters.get(value);
        return formatter ? formatter.header(value) : null;
    },
    hasBody(value) {
        const formatter = formatters.get(value);
        return formatter?.body !== undefined;
    },
    body(value) {
        const formatter = formatters.get(value);
        return formatter?.body ? formatter.body(value) : null;
    }
})