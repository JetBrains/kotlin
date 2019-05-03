import {println, startsWith} from "./utils";

export type CliDescription = {
    version: string,
    bin: string,
    description: string,
    usage: string,
    args: {
        [k: string]: CliArgDescription,
    },
    freeArgsTitle: string
}

export type CliArgValues = {
    [k: string]: string[] | string,
    free: string[]
}

export type CliArgDescription = {
    keys: string[],
    help: string,
    values?: string[],
    valuesHelp?: string[],
    default?: string,
    single?: true
}

export class CliArgsParser {
    constructor(private description: CliDescription) {
    }

    printUsage() {
        const description = this.description;

        println(`${description.bin} v${description.version} - ${description.description}`);
        println();
        println(`Usage: ${description.bin} ${description.usage}`);
        println();
        for (let key in description.args) {
            const data = description.args[key];
            println('  ' + data.keys.join(', '));
            const indent = '    ';
            println(`${indent}${data.help}`);
            if (data.values && data.valuesHelp) {
                println(`${indent}Possible values:`);
                for (let i = 0; i < data.values.length; i++) {
                    const value = data.values[i];
                    const help = data.valuesHelp[i];
                    println(`${indent} - "${value}": ${help}`)
                }
            }
            if (data.default) println(`${indent}By default: ${data.default}`);
            println('')
        }
    }

    badArgsExit(message: string) {
        println(message);
        println();
        this.printUsage();
        process.exit(1)
    }

    parse(args: string[]): CliArgValues {
        const description = this.description;

        const result: CliArgValues = {
            free: []
        };
        for (let key in description.args) {
            if (!description.args[key].single) {
                result[key] = [];
            }
        }

        // process all arguments from left to right
        args: while (args.length != 0) {
            const arg = args.shift() as string;

            if (startsWith(arg, '--')) {
                for (let argName in description.args) {
                    const argDescription = description.args[argName];
                    if (argDescription.keys.indexOf(arg) != -1) {
                        if (args.length == 0) {
                            this.badArgsExit("Missed value after option " + arg);
                        }

                        const value = args.shift() as string;
                        if (argDescription.values && argDescription.values.indexOf(value) == -1) {
                            this.badArgsExit("Unsupported value for option " + arg);
                        }

                        if (argDescription.single) {
                            result[argName] = value;
                        } else {
                            (result[argName] as string[]).push(value);
                        }

                        continue args;
                    }
                }

                this.badArgsExit("Unknown option: " + arg);
            } else {
                result.free.push(arg)
            }
        }

        if (result.free.length == 0) {
            this.badArgsExit(`At least one ${description.freeArgsTitle} should be provided`)
        }

        return result
    }
}
