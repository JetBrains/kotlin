/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import typescript from '@rollup/plugin-typescript';
import nodeResolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import {terser} from "rollup-plugin-terser";

const pckg = require('./package.json');

export default [
    {
        input: './nodejs.ts',
        output: {
            file: 'lib/kotlin-test-nodejs-runner.js',
            format: 'cjs',
            banner: '#!/usr/bin/env node',
            sourcemap: true
        },
        plugins: plugins()
    },
    {
        input: './nodejs-empty.ts',
        output: {
            file: 'lib/kotlin-test-nodejs-empty-runner.js',
            format: 'cjs',
            banner: '#!/usr/bin/env node',
            sourcemap: true
        },
        plugins: plugins()
    },
    {
        input: './karma.ts',
        output: {
            file: 'lib/kotlin-test-karma-runner.js',
            format: 'cjs',
            sourcemap: true
        },
        plugins: plugins()
    },
    {
        input: './karma-kotlin-debug-plugin.js',
        output: {
            file: 'lib/karma-kotlin-debug-plugin.js',
            format: 'esm'
        }
    },
    {
        input: './karma-debug-runner.js',
        output: {
            file: 'lib/karma-debug-runner.js',
            format: 'cjs'
        }
    },
    {
        input: './karma-debug-framework.js',
        output: {
            file: 'lib/karma-debug-framework.js',
            format: 'cjs'
        }
    },
    {
        input: './karma-kotlin-reporter.js',
        external: ['path', 'util'],
        output: {
            file: 'lib/karma-kotlin-reporter.js',
            format: 'cjs'
        }
    },
    {
        input: './tc-log-appender.js',
        output: {
            file: 'lib/tc-log-appender.js',
            format: 'cjs'
        }
    },
    {
        input: './tc-log-error-webpack.js',
        output: {
            file: 'lib/tc-log-error-webpack.js',
            format: 'cjs'
        }
    },
    {
        input: './webpack-5-debug.js',
        output: {
            file: 'lib/webpack-5-debug.js',
            format: 'cjs'
        }
    },
    {
        input: './mocha-kotlin-reporter.js',
        external: ['path', 'util'],
        output: {
            file: 'lib/mocha-kotlin-reporter.js',
            format: 'cjs'
        }
    },
    {
        input: './detect-correct-browser.js',
        output: {
            file: 'lib/detect-correct-browser.js',
            format: 'esm'
        }
    },
]

function plugins() {
    return [
        nodeResolve({
                        jsnext: true,
                        main: true
                    }),
        commonjs(),
        typescript({
                       tsconfig: "tsconfig.json"
                   }),
        terser({
                   compress: {
                       toplevel: true,
                       global_defs: {
                           DEBUG: false,
                           VERSION: pckg.version,
                           BIN: Object.keys(pckg.bin)[0],
                           DESCRIPTION: pckg.description
                       }
                   }
               })
    ]
}