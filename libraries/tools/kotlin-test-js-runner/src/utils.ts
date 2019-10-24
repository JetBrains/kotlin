/**
 * From teamcity-service-messages.
 * Copyright 2013 Aaron Forsander
 */
export function newFlowId(): number {
    return Math.floor(Math.random() * (1e10 - 1e6 + 1)) + 1e6;
}

/**
 * From teamcity-service-messages.
 * Copyright 2013 Aaron Forsander
 */
export function dateTimeWithoutTimeZone(): string {
    // TeamCity not fully support ISO 8601 (see TW-36173) so we need to cut off 'Z' at the end.
    return new Date().toISOString().slice(0, -1);
}

/**
 * From lodash.
 * Copyright JS Foundation and other contributors <https://js.foundation/>
 */
export function startsWith(string: string, target: string) {
    return string.slice(0, target.length) == target;
}

/**
 * From lodash.
 * Copyright JS Foundation and other contributors <https://js.foundation/>
 */
export function trim(str: string): string {
    return str.replace(/^[\s\uFEFF\xA0]+|[\s\uFEFF\xA0]+$/g, '');
}

/**
 * Used to match `RegExp`
 * [syntax characters](http://ecma-international.org/ecma-262/7.0/#sec-patterns).
 */
const reRegExpChar = /[\\^$.*+?()[\]{}|]/g,
    reHasRegExpChar = RegExp(reRegExpChar.source);

/**
 * Escapes the `RegExp` special characters "^", "$", "\", ".", "*", "+",
 * "?", "(", ")", "[", "]", "{", "}", and "|" in `string`.
 *
 * From lodash.
 * Copyright JS Foundation and other contributors <https://js.foundation/>
 */
export function escapeRegExp(string: string) {
    return (string && reHasRegExpChar.test(string))
        ? string.replace(reRegExpChar, '\\$&')
        : string;
}

export function pushIfNotNull<T>(list: T[], value: T) {
    if (value !== null) list.push(value)
}

export function flatMap<T>(arr: T[], f: (item: T) => T[]): T[] {
    const result: T[] = [];
    arr.forEach(item => {
        f(item).forEach(x => {
            result.push(x)
        })
    });
    return result;
}

export function println(message ?: string) {
    console.log(message)
}