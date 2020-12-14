/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// <path-to-v8> js/js.engines/src/org/jetbrains/kotlin/js/engine/repl.js

/*
Some of non-standard APIs available in standalone JS engines:

            v8   sm  jsc
load         +    +    +    load and evaluate a file
print        +    +    +    print to stdout
printErr     +    +    +    print to stderr
read         +    +    +    read a file as a text (v8, sm, jsc) or binary (sm, jsc)
readline     +    +    +    read line from stdin
readbuffer   +    -    -    read a binary file in v8
quit         +    +    +    stop the process

V8:
https://v8.dev/docs/d8
https://github.com/v8/v8/blob/4b9b23521e6fd42373ebbcb20ebe03bf445494f9/src/d8.cc
https://riptutorial.com/v8/example/25393/useful-built-in-functions-and-objects-in-d8

SpiderMonkey:
https://developer.mozilla.org/en-US/docs/Mozilla/Projects/SpiderMonkey/Introduction_to_the_JavaScript_shell

JavaScriptCore:
https://trac.webkit.org/wiki/JSC
https://github.com/WebKit/webkit/blob/master/Source/JavaScriptCore/jsc.cpp
*/

/**
 * @type {number}
 */
let currentRealmIndex = Realm.current();

function resetRealm() {
    if (currentRealmIndex !== 0) Realm.dispose(currentRealmIndex);
    currentRealmIndex = Realm.createAllowCrossRealmAccess()
}

/**
 * @type {?Map<string, ?any>}
 */
let globalState = null;

function saveGlobalState() {
    globalState = new Map();
    const currentGlobal = Realm.global(currentRealmIndex)
    for (const k in currentGlobal) {
        globalState.set(k, currentGlobal[k]);
    }

    console.log(Array.from(globalState.entries()))
}

function restoreGlobalState() {
    if (globalState === null) throw Error("There is no saved state!")

    const currentGlobal = Realm.global(currentRealmIndex)

    console.log(Array.from(globalState.entries()))

    for (const k in currentGlobal) {
        let prev = globalState.get(k);
        if (prev !== currentGlobal[k]) {
            currentGlobal[k] = prev;
        }
    }
    globalState = null;
}

// To prevent accessing to current global state
resetRealm();

// noinspection InfiniteLoopJS
while (true) {
    let code = readline().replace(/\\n/g, '\n');

    try {
        switch (code) {
            case "!reset":
                resetRealm()
                break;
            case "!saveGlobalState":
                saveGlobalState();
                break;
            case "!restoreGlobalState":
                restoreGlobalState();
                break;
            default:
                print(Realm.eval(currentRealmIndex, code));
        }
    } catch(e) {
        printErr(e.stack != null ? e.stack : e.toString());
        printErr('\nCODE:\n' + code);
    }

    print('<END>');
}
