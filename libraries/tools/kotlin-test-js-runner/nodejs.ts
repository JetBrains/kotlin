import {CliArgsParser, getDefaultCliDescription} from "./src/CliArgsParser";
import {runWithFilteringAndConsoleAdapters} from "./src/Adapter";
import {KotlinTestRunner} from "./src/KotlinTestRunner";

const parser = new CliArgsParser(
    getDefaultCliDescription(),
    process.exit
);
const untypedArgs = parser.parse(process.argv);

const adapterTransformer: (current: KotlinTestRunner) => KotlinTestRunner = current =>
    runWithFilteringAndConsoleAdapters(current, untypedArgs);

(globalThis as any).kotlinTest = {
    adapterTransformer: adapterTransformer
}