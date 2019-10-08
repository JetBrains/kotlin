/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import {CliArgValues} from "./CliArgsParser";
import {KotlinTestRunner} from "./KotlinTestRunner";
import {configureFiltering} from "./CliFiltertingConfiguration";

export function getFilteringAdapter(
    initialAdapter: KotlinTestRunner,
    cliArgsValue: CliArgValues
): KotlinTestRunner {
    const args = {
        include: cliArgsValue.include as string[],
        exclude: cliArgsValue.exclude as string[],
    };

    let runner: KotlinTestRunner = initialAdapter;
    runner = configureFiltering(runner, args.include, args.exclude);

    return runner;
}