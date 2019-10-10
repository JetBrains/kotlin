import {CliArgsParser, getDefaultCliDescription} from "./src/CliArgsParser";
import {runWithFilteringAndConsoleAdapters} from "./src/Adapter";

const kotlin_test = require('kotlin-test');

const parser = new CliArgsParser(getDefaultCliDescription());

const defaultMochaArgs = [
    '--reporter',
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

const initialAdapter = kotlin_test.kotlin.test.detectAdapter_8be2vx$();
kotlin_test.setAdapter(runWithFilteringAndConsoleAdapters(initialAdapter, untypedArgs));