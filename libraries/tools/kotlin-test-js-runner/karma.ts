/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import {CliArgsParser, getDefaultCliDescription} from "./src/CliArgsParser";
import {getAdapter} from "./src/Adapter";

const kotlin_test = require('kotlin-test');

process.hrtime = require('browser-process-hrtime');
process.exit = (exitCode) => {
    throw new Error(`Exit with ${exitCode}`)
};


const processArgs = window.__karma__.config.args;

const cliDescription = getDefaultCliDescription();
cliDescription.freeArgsTitle = null;
const parser = new CliArgsParser(cliDescription);
const untypedArgs = parser.parse(processArgs);

const initialAdapter = kotlin_test.kotlin.test.detectAdapter_8be2vx$();
kotlin_test.setAdapter(getAdapter(initialAdapter, untypedArgs));