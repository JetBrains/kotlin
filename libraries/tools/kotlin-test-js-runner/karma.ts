/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import {CliArgsParser, getDefaultCliDescription} from "./src/CliArgsParser";
import {runWithFilteringAdapter} from "./src/Adapter";
import {TeamCityMessagesFlow} from "./src/TeamCityMessagesFlow";
import {runWithTeamCityConsoleAdapter} from "./src/KotlinTestTeamCityReporter";

const kotlin_test = require('kotlin-test');

process.exit = (exitCode) => {
    throw new Error(`Exit with ${exitCode}`)
};

const processArgs = window.__karma__.config.args;
const cliDescription = getDefaultCliDescription();
cliDescription.freeArgsTitle = null;
const parser = new CliArgsParser(cliDescription);
const untypedArgs = parser.parse(processArgs);

const initialAdapter = kotlin_test.kotlin.test.detectAdapter_8be2vx$();

const realConsoleLog = console.log;
const teamCity = new TeamCityMessagesFlow(null, (payload) => realConsoleLog(payload));
const teamCityAdapter = runWithTeamCityConsoleAdapter(initialAdapter, teamCity);

kotlin_test.setAdapter(runWithFilteringAdapter(teamCityAdapter, untypedArgs));

const resultFun = window.__karma__.result;
window.__karma__.result = function (result) {
    console.log(`--END_KOTLIN_TEST--\n${JSON.stringify(result)}`);
    resultFun(result)
};