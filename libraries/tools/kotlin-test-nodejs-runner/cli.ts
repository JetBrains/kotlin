import {TeamCityMessagesFlow} from "./src/TeamCityMessagesFlow";
import {directRunner, KotlinTestRunner} from "./src/KotlinTestRunner";
import {IgnoredTestSuitesReporting, runWithTeamCityReporter} from "./src/KotlinTestTeamCityReporter";
import {defaultCliArgsParser} from "./src/CliArgsParser";
import {configureFiltering} from "./src/CliFiltertingConfiguration";
import {hrTimer} from "./src/Timer";

const kotlin_test = require('kotlin-test');

const parser = defaultCliArgsParser;

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