import {KotlinTestRunner} from "./KotlinTestRunner";
import {TeamCityMessageData, TeamCityMessagesFlow} from "./TeamCityMessagesFlow";
import {format} from "util";

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
                const log = (type: string) => function (message?: any, ...optionalParams: any[]) {
                    teamCity.sendMessage(
                        "testStdOut",
                        withName(
                            name,
                            {
                                "out": `[${type}] ${format(message, ...optionalParams)}\n`
                            }
                        )
                    )
                };

                const logMethods = ['log', 'info', 'warn', 'error', 'debug'];

                const globalConsole = console as unknown as {
                    [method: string]: (message?: any, ...optionalParams: any[]) => void
                };

                revertLogMethods = logMethods
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