/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

const vm = require('vm');
const fs = require('fs');

var testFilePath = process.argv[2];

const sandbox = {};
vm.createContext(sandbox);
const code = fs.readFileSync(testFilePath, 'utf-8');

try {
    vm.runInContext(code, sandbox, testFilePath);
    vm.runInContext('main.box()', sandbox);
} catch (e) {
    // Ignore any exceptions
}
