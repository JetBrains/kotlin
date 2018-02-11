#!/usr/bin/env node
/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var spawn = require('child_process').spawn;

var execPath = __dirname + '/kotlinc-js';
var args = process.argv.slice(2);

spawn('"' + execPath + '"', args, { stdio: "inherit", shell: true }).on('exit', function(exitCode) {
    process.exit(exitCode);
});
