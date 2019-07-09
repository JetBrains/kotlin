import {KotlinTestRunner} from "./KotlinTestRunner";
import {TeamCityMessageData, TeamCityMessagesFlow} from "./TeamCityMessagesFlow";
import {Timer} from "./Timer";

const kotlin_test = require('kotlin-test');

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

export function runWithTeamCityReporter(
    runner: KotlinTestRunner,
    ignoredTestSuites: IgnoredTestSuitesReporting,
    teamCity: TeamCityMessagesFlow,
    timer: Timer<any> | null
): KotlinTestRunner {
    let inIgnoredSuite = false;
    let currentAssertionResult: { expected: any, actual: any } | null = null;

    kotlin_test.kotlin.test.setAssertHook_4duqou$(function (assertionResult: { expected: any, actual: any }) {
        currentAssertionResult = assertionResult;
    });

    return {
        suite: function (name: string, isIgnored: boolean, fn: () => void) {
            if (isIgnored) {
                if (ignoredTestSuites == IgnoredTestSuitesReporting.skip) return;
                else if (ignoredTestSuites == IgnoredTestSuitesReporting.reportAsIgnoredTest) {
                    teamCity.sendMessage('testIgnored', withName(name, {"suite": true}));
                    return
                }
            }

            teamCity.sendMessage('testSuiteStarted', withName(name));

            // noinspection UnnecessaryLocalVariableJS
            const alreadyInIgnoredSuite = inIgnoredSuite;
            if (!alreadyInIgnoredSuite && isIgnored) {
                inIgnoredSuite = true;
            }

            try {
                if (isIgnored && ignoredTestSuites == IgnoredTestSuitesReporting.reportAllInnerTestsAsIgnored) {
                    fn();
                } else {
                    runner.suite(name, isIgnored, fn)
                }
            } finally {
                if (isIgnored && !alreadyInIgnoredSuite) {
                    inIgnoredSuite = false;
                }

                const data: TeamCityMessageData = withName(name);

                // extension only for Gradle
                if (isIgnored) data["ignored"] = true;

                teamCity.sendMessage('testSuiteFinished', data);
            }
        },
        test: function (name: string, isIgnored: boolean, fn: () => void) {
            if (inIgnoredSuite || isIgnored) {
                teamCity.sendMessage('testIgnored', withName(name));
            } else {
                const startTime = timer ? timer.start() : null;
                teamCity.sendMessage('testStarted', withName(name));
                try {
                    runner.test(name, isIgnored, fn);
                } catch (e) {
                    const data: TeamCityMessageData = withName(name, {
                        "message": e.message,
                        "details": e.stack
                    });

                    if (currentAssertionResult) {
                        data["type"] = 'comparisonFailure';
                        data["expected"] = currentAssertionResult.expected;
                        data["actual"] = currentAssertionResult.actual;
                    }
                    teamCity.sendMessage('testFailed', data);
                } finally {
                    currentAssertionResult = null;
                    const data: TeamCityMessageData = withName(name);
                    if (startTime) {
                        data["duration"] = timer!!.end(startTime); // ns to ms
                    }
                    teamCity.sendMessage('testFinished', data);
                }
            }
        }
    }
}