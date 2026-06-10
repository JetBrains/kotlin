import type DefaultValueClass from "./value-classes-cross-module-lib_v5.mjs";
import { createMainValue, echoLibValue } from "./value-classes-cross-module-lib_v5.mjs";
import type { LibValue, MainExternalInterface } from "./value-classes-cross-module-lib_v5.mjs";
import type { echoDefaultValueClass } from "./value-classes-cross-module-lib_v5.mjs";

type EchoDefaultValueClass = typeof echoDefaultValueClass;

function checkDefaultExportShape(value: DefaultValueClass, echo: EchoDefaultValueClass): string {
    const echoed: DefaultValueClass = echo(value);
    return echoed.value;
}

function checkExternalInterfaceShape(externalValue: MainExternalInterface): boolean {
    const directValue: number = externalValue.directValue;
    const nullableValue: MainExternalInterface["nullableValue"] = externalValue.nullableValue;
    const echoedValue: number = externalValue.echo(directValue);

    return echoedValue === directValue || nullableValue === undefined;
}

function checkMainModuleExports(): number {
    const mainValue: LibValue = createMainValue(42);
    const echoedValue: LibValue = echoLibValue(mainValue);

    return echoedValue.value;
}

export function box(): string {
    if (checkMainModuleExports() !== 42) return "FAIL";

    return "OK";
}
