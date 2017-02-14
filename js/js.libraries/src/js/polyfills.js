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

if (typeof String.prototype.startsWith === "undefined") {
    String.prototype.startsWith = function(searchString, position) {
        position = position || 0;
        return this.lastIndexOf(searchString, position) === position;
    };
}
if (typeof String.prototype.endsWith === "undefined") {
    String.prototype.endsWith = function(searchString, position) {
        var subjectString = this.toString();
        if (position === undefined || position > subjectString.length) {
            position = subjectString.length;
        }
        position -= searchString.length;
        var lastIndex = subjectString.indexOf(searchString, position);
        return lastIndex !== -1 && lastIndex === position;
    };
}
(function() {
    var normalizeOffset = function(offset, length) {
        if (offset < 0) return Math.max(0, offset + length);
        return Math.min(offset, length);
    };
    var typedArraySlice = function(begin, end) {
        if (typeof end === "undefined") {
            end = this.length;
        }
        begin = normalizeOffset(begin || 0, this.length);
        end = Math.max(begin, normalizeOffset(end, this.length));
        return new this.constructor(this.subarray(begin, end));
    };

    var arrays = [Int8Array, Int16Array, Int32Array, Float32Array, Float64Array];
    for (var i = 0; i < arrays.length; ++i) {
        var TypedArray = arrays[i];
        if (typeof TypedArray.prototype.slice === "undefined") {
            Object.defineProperty(TypedArray.prototype, 'slice', {
                value: typedArraySlice
            });
        }
    }
})();

// var check = function(a, b, l, r) {
//     if (a.length != b.length) console.log("botva " + l + " " + r);
//     for (var i = 0; i < a.length; ++i) {
//         if (a[i] !== b[i]) console.log("botva " + l + " " + r);
//     }
// };
// for (var i = -10; i < 10; ++i) {
//     check(a.slice(i), b.slice(i), i);
//     for (var j = -10; j < 10; ++j) {
//         check(a.slice(i, j), b.slice(i, j), i, j);
//     }
// }