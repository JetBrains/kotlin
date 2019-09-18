import {defaultCliArgsParser} from "./src/CliArgsParser";
import {getAdapter} from "./src/Adapter";
import {directRunner} from "./src/KotlinTestRunner";

const kotlin_test = require('kotlin-test');

const parser = defaultCliArgsParser;

const processArgs = process.argv.slice(2);
const untypedArgs = parser.parse(processArgs);
kotlin_test.setAdapter(getAdapter(directRunner, untypedArgs));

untypedArgs.free.forEach((arg: string) => {
    require(arg);
});