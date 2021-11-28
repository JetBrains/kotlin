/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

let instance;
let heap;
let global_arguments;

function isBrowser() {
    return typeof self !== 'undefined';
}

let runtime;
if (isBrowser()) {
    runtime = {
        print: console.log,
        stdout: '',
        write: function (message) {
            this.stdout += message;
            const lastNewlineIndex = this.stdout.lastIndexOf('\n');
            if (lastNewlineIndex == -1) return;
            this.print(this.stdout.substring(0, lastNewlineIndex));
            this.stdout = this.stdout.substring(lastNewlineIndex + 1)
        },
        flush: function () {
            this.print(this.stdout);
        },
        exit: function (status) {
            throw Error("Kotlin process called exit (" + status + ")");
        }
    };
} else {
    runtime = {
        write: write,
        print: print,
        flush: function () {
        },
        exit: quit
    };
}

function print_usage() {
    // TODO: any reliable way to obtain the current script name?
    runtime.print('Usage: d8 --expose-wasm launcher.js -- <program.wasm> <program arg1> <program arg2> ...')
    quit(1); // TODO: this is d8 specific
}

function utf8encode(s) {
    return unescape(encodeURIComponent(s));
}

function utf8decode(s) {
    return decodeURIComponent(escape(s));
}

function fromString(string, pointer) {
    for (let i = 0; i < string.length; i++) {
        heap[pointer + i] = string.charCodeAt(i);
    }
    heap[pointer + string.length] = 0;
}

function toString(pointer) {
    let string = '';
    for (let i = pointer; heap[i] != 0; i++) {
        string += String.fromCharCode(heap[i]);
    }
    return string;
}

function toUTF16String(pointer, size) {
    let string = '';
    for (let i = pointer; i < pointer + size; i += 2) {
        string += String.fromCharCode(heap[i] + heap[i + 1] * 256);
    }
    return string;
}

function twoIntsToDouble(upper, lower) {
    const buffer = new ArrayBuffer(8);
    const ints = new Int32Array(buffer);
    const doubles = new Float64Array(buffer);
    ints[1] = upper;
    ints[0] = lower;
    return doubles[0];
}

function doubleToTwoInts(value) {
    const buffer = new ArrayBuffer(8);
    const ints = new Int32Array(buffer);
    const doubles = new Float64Array(buffer);
    doubles[0] = value;
    return {upper: ints[1], lower: ints[0]}
}

function int32ToHeap(value, pointer) {
    heap[pointer] = value & 0xff;
    heap[pointer + 1] = (value & 0xff00) >>> 8;
    heap[pointer + 2] = (value & 0xff0000) >>> 16;
    heap[pointer + 3] = (value & 0xff000000) >>> 24;
}

function doubleToReturnSlot(value) {
    const twoInts = doubleToTwoInts(value);
    instance.exports.ReturnSlot_setDouble(twoInts.upper, twoInts.lower);
}

let konan_dependencies = {
    env: {
        abort: function () {
            throw new Error("abort()");
        },
        // TODO: Account for file and size.
        fgets: function (str, size, file) {
            // TODO: readline can't read lines without a newline.
            // Browsers cant read from console at all.
            fromString(utf8encode(readline() + '\n'), str);
            return str;
        },
        read: function (file, str, size) {
            let string = utf8encode(readline() + '\n');
            fromString(string.substring(0, size), str);
            return string.length;
        },
        Konan_notify_memory_grow: function() {
            heap = new Uint8Array(instance.exports.memory.buffer);
        },
        Konan_abort: function (pointer) {
            throw new Error("Konan_abort(" + utf8decode(toString(pointer)) + ")");
        },
        Konan_exit: function (status) {
            runtime.exit(status);
        },
        Konan_js_arg_size: function (index) {
            if (index >= global_arguments.length) return -1;
            return global_arguments[index].length + 1; // + 1 for trailing zero.
        },
        Konan_js_fetch_arg: function (index, ptr) {
            let arg = utf8encode(global_arguments[index]);
            fromString(arg, ptr);
        },
        Konan_date_now: function (pointer) {
            let now = Date.now();
            let high = Math.floor(now / 0xffffffff);
            let low = Math.floor(now % 0xffffffff);
            int32ToHeap(low, pointer);
            int32ToHeap(high, pointer + 4);
        },
        // TODO: Account for fd and size.
        write: function (fd, str, size) {
            if (fd != 1 && fd != 2) throw ("write(" + fd + ", ...)");
            // TODO: There is no writeErr() in d8.
            // Approximate it with write() to stdout for now.
            runtime.write(utf8decode(toString(str)));
        },
        fflush: function(file) {
            runtime.flush();
        }
    }
};

function linkJavaScriptLibraries() {
    konan.libraries.forEach(function (library) {
        for (const property in library) {
            konan_dependencies.env[property] = library[property];
        }
    });
}

function invokeModule(inst, args) {
    if (args.length < 1) print_usage();
    global_arguments = args;

    instance = inst;

    heap = new Uint8Array(instance.exports.memory.buffer);

    let exit_status = 0;

    try {
        exit_status = instance.exports.Konan_js_main(args.length, isBrowser() ? 0 : 1);
    } catch (e) {
        runtime.print("Exception executing entry point: " + e);
        runtime.print(e.stack);
        exit_status = 1;
    }
    runtime.flush();

    return exit_status;
}

// Instantiate module in Browser.
function instantiateAndRun(arraybuffer, args) {
    linkJavaScriptLibraries();
    WebAssembly.instantiate(arraybuffer, konan_dependencies)
        .then(resultObject => invokeModule(resultObject.instance, args));
}

// Instantiate module in d8 synchronously.
function instantiateAndRunSync(arraybuffer, args) {
    const module = new WebAssembly.Module(arraybuffer);
    linkJavaScriptLibraries();
    const instance = new WebAssembly.Instance(module, konan_dependencies);
    return invokeModule(instance, args)
}


// Instantiate module in Browser using streaming instantiation.
function instantiateAndRunStreaming(filename) {
    linkJavaScriptLibraries();
    WebAssembly.instantiateStreaming(fetch(filename), konan_dependencies)
        .then(resultObject => invokeModule(resultObject.instance, [filename]));
}

konan.moduleEntry = function (args) {
    if (isBrowser()) {
        if (!document.currentScript.hasAttribute("wasm")) {
            throw new Error('Could not find the wasm attribute pointing to the WebAssembly binary.');
        }
        const filename = document.currentScript.getAttribute("wasm");
        if (typeof WebAssembly.instantiateStreaming === 'function') {
            instantiateAndRunStreaming(filename);
        } else {
            fetch(filename)
              .then(response => response.arrayBuffer())
              .then(arraybuffer => instantiateAndRun(arraybuffer, [filename]));
        }
    } else {
        // Invoke from d8.
        const arrayBuffer = readbuffer(args[0]);
        const exitStatus = instantiateAndRunSync(arrayBuffer, args);
        quit(exitStatus);
    }
};

