/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import {KotlinTestRunner} from "./KotlinTestRunner";
import {TeamCityMessageData, TeamCityMessagesFlow} from "./TeamCityMessagesFlow";
const format = require("format-util");

// don't use enum as it is not minified by uglify
export type IgnoredTestSuitesReporting
    = "skip" | "reportAsIgnoredTest" | "reportAllInnerTestsAsIgnored"
export const IgnoredTestSuitesReporting: { [key: string]: IgnoredTestSuitesReporting } = {
    skip: "skip",
    reportAsIgnoredTest: "reportAsIgnoredTest",
    reportAllInnerTestsAsIgnored: "reportAllInnerTestsAsIgnored"
};

// to reduce minified code size
function withName(name: string, data?: TeamCityMessageData): TeamCityMessageData {
    data = data || {};
    data["name"] = name;
    return data
}

const logTypes = ['log', 'info', 'warn', 'error', 'debug'] as const

type LogType = typeof logTypes[number]

export function runWithTeamCityConsoleAdapter(
    runner: KotlinTestRunner,
    teamCity: TeamCityMessagesFlow
): KotlinTestRunner {
    return {
        suite: function (name: string, isIgnored: boolean, fn: () => void) {
            runner.suite(name, isIgnored, fn)
        },
        test: function (name: string, isIgnored: boolean, fn: () => void) {
            let revertLogMethods: CallableFunction[] = [];

            runner.test(name, isIgnored, () => {
                const log = (type: LogType) => function (message?: any, ...optionalParams: any[]) {
                    let messageType: 'testStdOut' | 'testStdErr'
                    if (type == 'warn' || type == 'error') {
                        messageType = 'testStdErr'
                    } else {
                        messageType = 'testStdOut'
                    }

                    teamCity.sendMessage(
                        messageType,
                        withName(
                            name,
                            {
                                "out": `[${type}] ${format(message, ...optionalParams)}\n`
                            }
                        )
                    )
                };

                const globalConsole = console as unknown as {
                    [method: string]: (message?: any, ...optionalParams: any[]) => void
                };

                revertLogMethods = logTypes
                    .map(method => {
                        const realMethod = globalConsole[method];
                        globalConsole[method] = log(method);
                        return () => globalConsole[method] = realMethod
                    });
                try {
                    return fn();
                } catch (e) {
                    throw e;
                } finally {
                    revertLogMethods.forEach(revert => revert());
                }
            });
        }
    }
}