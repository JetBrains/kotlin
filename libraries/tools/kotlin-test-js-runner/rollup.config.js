import typescript from 'rollup-plugin-typescript2';
import nodeResolve from 'rollup-plugin-node-resolve';
import commonjs from 'rollup-plugin-commonjs';
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
        input: './nodejs-source-map-support.js',
        external: ['path', 'fs', 'module'],
        output: {
            file: 'lib/kotlin-nodejs-source-map-support.js',
            format: 'cjs',
            sourcemap: true
        },
        plugins: [
            nodeResolve({
                            jsnext: true,
                            main: true
                        }),
            commonjs(),
            terser({
                       compress: true,
                       sourcemap: true
                   })
        ]
    },
    {
        input: './karma.ts',
        output: {
            file: 'lib/kotlin-test-karma-runner.js',
            format: 'cjs',
            sourcemap: true
        },
        plugins: plugins()
    }
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
                   sourcemap: true,
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