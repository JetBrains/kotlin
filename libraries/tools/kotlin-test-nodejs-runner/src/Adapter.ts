/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import {CliArgValues} from "./CliArgsParser";
import {IgnoredTestSuitesReporting, runWithTeamCityReporter} from "./KotlinTestTeamCityReporter";
import {TeamCityMessagesFlow} from "./TeamCityMessagesFlow";
import {KotlinTestRunner} from "./KotlinTestRunner";
import {hrTimer} from "./Timer";
import {configureFiltering} from "./CliFiltertingConfiguration";

export function getAdapter(
    initialAdapter: KotlinTestRunner,
    cliArgsValue: CliArgValues
): KotlinTestRunner {
    const args = {
        onIgnoredTestSuites: (cliArgsValue.ignoredTestSuites
            || IgnoredTestSuitesReporting.reportAllInnerTestsAsIgnored) as IgnoredTestSuitesReporting,
        include: cliArgsValue.include as string[],
        exclude: cliArgsValue.exclude as string[],
    };

    const teamCity = new TeamCityMessagesFlow(null, (payload) => console.log(payload));

    let runner: KotlinTestRunner = initialAdapter;
    runner = runWithTeamCityReporter(runner, args.onIgnoredTestSuites, teamCity, hrTimer);
    runner = configureFiltering(runner, args.include, args.exclude);

    return runner;
}