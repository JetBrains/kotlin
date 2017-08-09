/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

var module;
var instance;
var heap;
var memory;
var global_arguments = arguments;
var exit_status = 0;
var globalBase = 0; // Is there any way to obtain global_base from JavaScript?

function print_usage() {
    // TODO: any reliable way to obtain the current script name?
    print('Usage: d8 --expose-wasm launcher.js -- <program.wasm> <program arg1> <program arg2> ...')
    quit(1); // TODO: this is d8 specific
}

function utf8encode(s) {
    return unescape(encodeURIComponent(s));
}

function utf8decode(s) {
    return decodeURIComponent(escape(s));
}

function fromString(string, pointer) {
    for (i = 0; i < string.length; i++) {
        heap[pointer + i] = string.charCodeAt(i);
    }
    heap[pointer + string.length] = 0;
}

function toString(pointer) {
    var string = '';
    for (var i = pointer; heap[i] != 0; i++) {
        string += String.fromCharCode(heap[i]);
    }
    return string;
}

function int32ToHeap(value, pointer) {
    heap[pointer]   = value & 0xff;
    heap[pointer+1] = (value & 0xff00) >>> 8;
    heap[pointer+2] = (value & 0xff0000) >>> 16;
    heap[pointer+3] = (value & 0xff000000) >>> 24;
}

function stackTop() {
    // Read the value module's `__stack_pointer` is initialized with.
    // It is the very first static in .data section.
    var addr = (globalBase == 0 ? 4 : globalBase);
    var fourBytes = heap.buffer.slice(addr, addr+4);
    return new Uint32Array(fourBytes)[0];
}

function runGlobalInitializers(exports) {
    for (var property in exports) {
        if (property.startsWith("Konan_global_ctor_")) {
            exports[property]();
        }
    }
}

var konan_dependencies = {
    env: {
        abort: function() {
            throw new Error("abort()");
        },
        // TODO: Account for file and size.
        fgets: function(str, size, file) {
            // TODO: readline can't read lines without a newline.
            // Browsers cant read from console at all.
            fromString(utf8encode(readline() + '\n'), str);
            return str;
        },
        Konan_heap_upper: function() {
            return memory.buffer.byteLength;
        },
        Konan_heap_lower: function() {
            return konanStackTop;
        },
        Konan_heap_grow: function(pages) {
            // The buffer is allocated anew on calls to grow(),
            // so renew the heap array.
            var oldLength = memory.grow(pages);
            heap = new Uint8Array(konan_dependencies.env.memory.buffer);
            return oldLength;
        },
        Konan_abort: function(pointer) {
            throw new Error("Konan_abort(" + utf8decode(toString(pointer)) + ")");
        },
        Konan_js_arg_size: function(index) {
            if (index >= global_arguments.length) return -1;
            return global_arguments[index].length + 1; // + 1 for trailing zero.
        },
        Konan_js_fetch_arg: function(index, ptr) {
            var arg = utf8encode(global_arguments[index]);
            fromString(arg, ptr);
        },
        Konan_date_now: function(pointer) {
            var now = Date.now();
            var high = Math.floor(now / 0xffffffff);
            var low = Math.floor(now % 0xffffffff);
            int32ToHeap(low, pointer);
            int32ToHeap(high, pointer+4);
        },
        stdin: 0, // This is for fgets(,,stdin) to resolve. It is ignored.
        // TODO: Account for fd and size.
        write: function(fd, str, size) {
            if (fd != 1 && fd != 2) throw ("write(" + fd + ", ...)");
            // TODO: There is no writeErr() in d8. 
            // Approximate it with write() to stdout for now.
            write(utf8decode(toString(str))); // TODO: write() d8 specific.
        },
        memory: new WebAssembly.Memory({ initial: 256, maximum: 16384 })
    }
};

if (arguments.length < 1) print_usage();

module = new WebAssembly.Module(new Uint8Array(readbuffer(arguments[0])));
module.env = {};
module.env.memoryBase = 0;
module.env.tablebase = 0;

instance = new WebAssembly.Instance(module, konan_dependencies);
memory = konan_dependencies.env.memory
heap = new Uint8Array(konan_dependencies.env.memory.buffer);
konanStackTop = stackTop();

try {
    runGlobalInitializers(instance.exports);
    exit_status = instance.exports.Konan_js_main(arguments.length);
} catch (e) {
    print("Exception executing Konan_js_main: " + e);
    print(e.stack);
    exit_status = 1;
}

quit(exit_status); // TODO: d8 specific.

