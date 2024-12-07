/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import terser from '@rollup/plugin-terser';

import pckg from "./package.json" with { type: 'json' };

export default [
    {
        input: './src/index.mjs',
        output: {
            file: './build/out/custom-formatters.js',
        },
        plugins: [
            terser({
                compress: {
                    toplevel: true,
                    global_defs: {
                        DEBUG: false,
                        VERSION: pckg.version,
                        DESCRIPTION: pckg.description
                    }
                }
            })
        ]
    },
]