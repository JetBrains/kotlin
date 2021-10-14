import {CliArgsParser, getDefaultCliDescription} from "./src/CliArgsParser";
import {runWithFilteringAndConsoleAdapters} from "./src/Adapter";

let kotlin_test
try {
    kotlin_test = require('kotlin-test');
} catch {
}

if (kotlin_test) {
    const parser = new CliArgsParser(
        getDefaultCliDescription(),
        process.exit
    );
    const untypedArgs = parser.parse(process.argv);

    const initialAdapter = kotlin_test.kotlin.test.detectAdapter_8be2vx$();
    kotlin_test.setAdapter(runWithFilteringAndConsoleAdapters(initialAdapter, untypedArgs));
}