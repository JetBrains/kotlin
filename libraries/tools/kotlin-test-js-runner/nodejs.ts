import {CliArgsParser, getDefaultCliDescription} from "./src/CliArgsParser";
import {runWithFilteringAdapter} from "./src/Adapter";
import {runWithTeamCityConsoleAdapter} from "./src/KotlinTestTeamCityReporter";
import {TeamCityMessagesFlow} from "./src/TeamCityMessagesFlow";

const kotlin_test = require('kotlin-test');

const parser = new CliArgsParser(getDefaultCliDescription());

const defaultMochaArgs = [
    '--reporter',
    'place-holder',
    '--require',
    'place-holder',
    '--require',
    'place-holder',
    '--no-config',
    '--no-package',
    '--no-opts',
    '--diff',
    '--extension',
    'js',
    '--slow',
    '75',
    '--timeout',
    '2000',
    '--ui',
    'bdd'
];

const processArgs = process.argv.slice(2, -1 * defaultMochaArgs.length);
const untypedArgs = parser.parse(processArgs);

// TODO(ilgonmic): Try to detect adapter
const initialAdapter = new kotlin_test.kotlin.test.adapters.JasmineLikeAdapter();

const realConsoleLog = console.log;
const teamCity = new TeamCityMessagesFlow(null, (payload) => realConsoleLog(payload));
const teamCityAdapter = runWithTeamCityConsoleAdapter(initialAdapter, teamCity);
kotlin_test.setAdapter(runWithFilteringAdapter(teamCityAdapter, untypedArgs));