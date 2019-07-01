import {TeamCityMessagesFlow} from "./src/TeamCityMessagesFlow";
import {directRunner, KotlinTestRunner} from "./src/KotlinTestRunner";
import {IgnoredTestSuitesReporting, runWithTeamCityReporter} from "./src/KotlinTestTeamCityReporter";
import {CliArgsParser} from "./src/CliArgsParser";
import {configureFiltering} from "./src/CliFiltertingConfiguration";
import {hrTimer} from "./src/Timer";

const kotlin_test = require('kotlin-test');

const parser = new CliArgsParser({
    version: VERSION,
    bin: BIN,
    description: DESCRIPTION,
    usage: "[-t --tests] [-e --exclude] <module_name1>, <module_name2>, ..",
    args: {
        include: {
            keys: ['--tests', '--include'],
            help: "Tests to include. Example: MySuite.test1,MySuite.MySubSuite.*,*unix*,!*windows*",
            default: "*"

        },
        exclude: {
            keys: ['--exclude'],
            help: "Tests to exclude. Example: MySuite.test1,MySuite.MySubSuite.*,*unix*"
        },
        ignoredTestSuites: {
            keys: ['--ignoredTestSuites'],
            help: "How to deal with ignored test suites",
            single: true,
            values: [
                IgnoredTestSuitesReporting.skip,
                IgnoredTestSuitesReporting.reportAsIgnoredTest,
                IgnoredTestSuitesReporting.reportAllInnerTestsAsIgnored
            ],
            valuesHelp: [
                "don't report ignored test suites",
                "useful to speedup large ignored test suites",
                "will cause visiting all inner tests",
            ],
            default: IgnoredTestSuitesReporting.reportAllInnerTestsAsIgnored
        }
    },
    freeArgsTitle: "module_name"
});

const processArgs = process.argv.slice(2);
const untypedArgs = parser.parse(processArgs);
const args = {
    onIgnoredTestSuites: (untypedArgs.ignoredTestSuites
        || IgnoredTestSuitesReporting.reportAllInnerTestsAsIgnored) as IgnoredTestSuitesReporting,
    include: untypedArgs.include as string[],
    exclude: untypedArgs.exclude as string[],
};

const teamCity = new TeamCityMessagesFlow(null, (payload) => console.log(payload));

let runner: KotlinTestRunner = directRunner;
runner = runWithTeamCityReporter(runner, args.onIgnoredTestSuites, teamCity, hrTimer);
runner = configureFiltering(runner, args.include, args.exclude);

kotlin_test.setAdapter(runner);

untypedArgs.free.forEach((arg: string) => {
    require(arg);
});