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

(function() {
    function normalizeOffset(offset, length) {
        if (offset < 0) return Math.max(0, offset + length);
        return Math.min(offset, length);
    }
    function typedArraySlice(begin, end) {
        if (typeof end === "undefined") {
            end = this.length;
        }
        begin = normalizeOffset(begin || 0, this.length);
        end = Math.max(begin, normalizeOffset(end, this.length));
        return new this.constructor(this.subarray(begin, end));
    }

    var arrays = [Int8Array, Int16Array, Uint16Array, Int32Array, Float32Array, Float64Array];
    for (var i = 0; i < arrays.length; ++i) {
        var TypedArray = arrays[i];
        if (typeof TypedArray.prototype.slice === "undefined") {
            Object.defineProperty(TypedArray.prototype, 'slice', {
                value: typedArraySlice
            });
        }
    }
})();