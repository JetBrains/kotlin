import {CliArgsParser, getDefaultCliDescription} from "./src/CliArgsParser";
import {getFilteringAdapter} from "./src/Adapter";
import {directRunner} from "./src/KotlinTestRunner";
import {IgnoredTestSuitesReporting, runWithTeamCityReporter} from "./src/KotlinTestTeamCityReporter";
import {hrTimer} from "./src/Timer";
import {TeamCityMessagesFlow} from "./src/TeamCityMessagesFlow";

const kotlin_test = require('kotlin-test');

const parser = new CliArgsParser(getDefaultCliDescription());

const processArgs = process.argv.slice(2);
const untypedArgs = parser.parse(processArgs);

const realConsoleLog = console.log;
const teamCity = new TeamCityMessagesFlow(null, (payload) => realConsoleLog(payload));

const onIgnoredTestSuites = (untypedArgs.ignoredTestSuites
    || IgnoredTestSuitesReporting.reportAllInnerTestsAsIgnored) as IgnoredTestSuitesReporting;

const runner = runWithTeamCityReporter(directRunner, onIgnoredTestSuites, teamCity, hrTimer);
kotlin_test.setAdapter(getFilteringAdapter(runner, untypedArgs));

untypedArgs.free.forEach((arg: string) => {
    require(arg);
});