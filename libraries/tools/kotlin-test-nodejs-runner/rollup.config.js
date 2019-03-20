import typescript from 'rollup-plugin-typescript2';
import {uglify} from "rollup-plugin-uglify";
import nodeResolve from 'rollup-plugin-node-resolve';
import commonjs from 'rollup-plugin-commonjs';

const pckg = require('./package.json');

export default [
    {
        input: './cli.ts',
        output: {
            file: 'lib/kotlin-test-nodejs-runner.js',
            format: 'cjs',
            banner: '#!/usr/bin/env node'
        },
        plugins: [
            nodeResolve({
                jsnext: true,
                main: true
            }),
            commonjs(),
            typescript({
                tsconfig: "tsconfig.json"
            }),
            uglify({
                sourcemap: true,
                compress: {
                    // hoist_funs: true,
                    // hoist_vars: true,
                    toplevel: true,
                    unsafe: true,
                    dead_code: true,
                    global_defs: {
                        DEBUG: false,
                        VERSION: pckg.version,
                        BIN: Object.keys(pckg.bin)[0],
                        DESCRIPTION: pckg.description
                    }
                },
                mangle: {
                    properties: {
                        keep_quoted: true,
                        reserved: [
                            "argv", "hrtime",
                            "kotlin_test", "kotlin", "setAdapter", "setAssertHook_4duqou$",
                            "suite", "test",
                            "stack"
                        ]
                    },
                    toplevel: true,
                },
                // output: {
                //     beautify: true
                // }
            }),
            // sourceMaps()
        ]
    }
]