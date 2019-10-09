import {CliArgsParser, getDefaultCliDescription} from "./src/CliArgsParser";
import {getFilteringAdapter} from "./src/Adapter";

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

const initialAdapter = new kotlin_test.kotlin.test.adapters.JasmineLikeAdapter();
kotlin_test.setAdapter(getFilteringAdapter(initialAdapter, untypedArgs));