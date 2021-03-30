/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import {CliArgsParser, getDefaultCliDescription} from "./src/CliArgsParser";
import {runWithFilteringAndConsoleAdapters} from "./src/Adapter";

const kotlin_test = require('kotlin-test');

const parser = new CliArgsParser(
    getDefaultCliDescription(),
    (exitCode) => {
        throw new Error(`Exit with ${exitCode}`)
    }
);
const untypedArgs = parser.parse(window.__karma__.config.args);

const initialAdapter = kotlin_test.kotlin.test.detectAdapter_8be2vx$();
kotlin_test.setAdapter(runWithFilteringAndConsoleAdapters(initialAdapter, untypedArgs));

const resultFun = window.__karma__.result;
window.__karma__.result = function (result) {
    console.log(`--END_KOTLIN_TEST--\n${JSON.stringify(result)}`);
    resultFun(result)
};