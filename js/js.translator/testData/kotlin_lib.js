/**
 * Copyright 2010 Tim Down.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function (Kotlin) {
    "use strict";

    var CharSequence = Kotlin.createTraitNow(null);

    // Shims for String
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

    String.prototype.contains = function (s) {
        return this.indexOf(s) !== -1;
    };

    // Kotlin stdlib

    Kotlin.equals = function (obj1, obj2) {
        if (obj1 == null) {
            return obj2 == null;
        }

        if (obj2 == null) {
            return false;
        }

        if (Array.isArray(obj1)) {
            return Kotlin.arrayEquals(obj1, obj2);
        }

        if (typeof obj1 == "object" && typeof obj1.equals_za3rmp$ === "function") {
            return obj1.equals_za3rmp$(obj2);
        }

        return obj1 === obj2;
    };

    Kotlin.hashCode = function (obj) {
        if (obj == null) {
            return 0;
        }
        if ("function" == typeof obj.hashCode) {
            return obj.hashCode();
        }
        var objType = typeof obj;
        if ("object" == objType || "function" == objType) {
            return getObjectHashCode(obj);
        } else if ("number" == objType) {
            // TODO: a more elaborate code is needed for floating point values.
            return obj | 0;
        } if ("boolean" == objType) {
            return Number(obj)
        }

        var str = String(obj);
        return getStringHashCode(str);
    };

    Kotlin.toString = function (o) {
        if (o == null) {
            return "null";
        }
        else if (Array.isArray(o)) {
            return Kotlin.arrayToString(o);
        }
        else {
            return o.toString();
        }
    };

    Kotlin.arrayToString = function (a) {
        return "[" + a.map(Kotlin.toString).join(", ") + "]";
    };

    Kotlin.compareTo = function (a, b) {
        var typeA = typeof a;
        var typeB = typeof a;
        if (Kotlin.isChar(a) && typeB == "number") {
            return Kotlin.primitiveCompareTo(a.charCodeAt(0), b);
        }
        if (typeA == "number" && Kotlin.isChar(b)) {
            return Kotlin.primitiveCompareTo(a, b.charCodeAt(0));
        }
        if (typeA == "number" || typeA == "string") {
            return a < b ? -1 : a > b ? 1 : 0;
        }
        return a.compareTo_za3rmp$(b);
    };

    Kotlin.primitiveCompareTo = function (a, b) {
        return a < b ? -1 : a > b ? 1 : 0;
    };

    Kotlin.isNumber = function (a) {
        return typeof a == "number" || a instanceof Kotlin.Long;
    };

    Kotlin.isChar = function (value) {
        return (typeof value) == "string" && value.length == 1;
    };

    Kotlin.isComparable = function (value) {
        var type = typeof value;

        return type === "string" ||
               type === "boolean" ||
               Kotlin.isNumber(value) ||
               Kotlin.isType(value, Kotlin.Comparable);
    };
    
    Kotlin.isCharSequence = function (value) {
        return typeof value === "string" || Kotlin.isType(value, CharSequence);
    };

    Kotlin.charInc = function (value) {
        return String.fromCharCode(value.charCodeAt(0)+1);
    };

    Kotlin.charDec = function (value) {
        return String.fromCharCode(value.charCodeAt(0)-1);
    };

    Kotlin.toShort = function (a) {
        return (a & 0xFFFF) << 16 >> 16;
    };

    Kotlin.toByte = function (a) {
        return (a & 0xFF) << 24 >> 24;
    };

    Kotlin.toChar = function (a) {
       return String.fromCharCode((((a | 0) % 65536) & 0xFFFF) << 16 >>> 16);
    };

    Kotlin.numberToLong = function (a) {
        return a instanceof Kotlin.Long ? a : Kotlin.Long.fromNumber(a);
    };

    Kotlin.numberToInt = function (a) {
        return a instanceof Kotlin.Long ? a.toInt() : (a | 0);
    };

    Kotlin.numberToShort = function (a) {
        return Kotlin.toShort(Kotlin.numberToInt(a));
    };

    Kotlin.numberToByte = function (a) {
        return Kotlin.toByte(Kotlin.numberToInt(a));
    };

    Kotlin.numberToDouble = function (a) {
        return +a;
    };

    Kotlin.numberToChar = function (a) {
        return Kotlin.toChar(Kotlin.numberToInt(a));
    };

    Kotlin.intUpto = function (from, to) {
        return new Kotlin.NumberRange(from, to);
    };

    Kotlin.intDownto = function (from, to) {
        return new Kotlin.Progression(from, to, -1);
    };

    Kotlin.Throwable = Error;


    function createClassNowWithMessage(base) {
        return Kotlin.createClassNow(base,
                   /** @constructs */
                   function (message) {
                       this.message = (message !== void 0) ? message : null;
                   }
               );
    }

    Kotlin.Error = createClassNowWithMessage(Kotlin.Throwable);
    Kotlin.Exception = createClassNowWithMessage(Kotlin.Throwable);
    Kotlin.RuntimeException = createClassNowWithMessage(Kotlin.Exception);
    Kotlin.NullPointerException = createClassNowWithMessage(Kotlin.RuntimeException);
    Kotlin.NoSuchElementException = createClassNowWithMessage(Kotlin.RuntimeException);
    Kotlin.IllegalArgumentException = createClassNowWithMessage(Kotlin.RuntimeException);
    Kotlin.IllegalStateException = createClassNowWithMessage(Kotlin.RuntimeException);
    Kotlin.UnsupportedOperationException = createClassNowWithMessage(Kotlin.RuntimeException);
    Kotlin.IndexOutOfBoundsException = createClassNowWithMessage(Kotlin.RuntimeException);
    Kotlin.ClassCastException = createClassNowWithMessage(Kotlin.RuntimeException);
    Kotlin.IOException = createClassNowWithMessage(Kotlin.Exception);
    Kotlin.AssertionError = createClassNowWithMessage(Kotlin.Error);

    Kotlin.throwNPE = function (message) {
        throw new Kotlin.NullPointerException(message);
    };

    Kotlin.throwCCE = function () {
        throw new Kotlin.ClassCastException("Illegal cast");
    };

    function throwAbstractFunctionInvocationError(funName) {
        return function () {
            var message;
            if (funName !== void 0) {
                message = "Function " + funName + " is abstract";
            }
            else {
                message = "Function is abstract";
            }
            throw new TypeError(message);
        };
    }

    /** @const */
    var POW_2_32 = 4294967296;
    // TODO: consider switching to Symbol type once we are on ES6.
    /** @const */
    var OBJECT_HASH_CODE_PROPERTY_NAME = "kotlinHashCodeValue$";

    function getObjectHashCode(obj) {
        if (!(OBJECT_HASH_CODE_PROPERTY_NAME in obj)) {
            var hash = (Math.random() * POW_2_32) | 0; // Make 32-bit singed integer.
            Object.defineProperty(obj, OBJECT_HASH_CODE_PROPERTY_NAME, { value:  hash, enumerable: false });
        }
        return obj[OBJECT_HASH_CODE_PROPERTY_NAME];
    }

    function getStringHashCode(str) {
        var hash = 0;
        for (var i = 0; i < str.length; i++) {
            var code  = str.charCodeAt(i);
            hash  = (hash * 31 + code) | 0; // Keep it 32-bit.
        }
        return hash;
    }

    var lazyInitClasses = {};

    /**
     * @class
     * @implements {Kotlin.Iterator.<T>}
     *
     * @constructor
     * @param {Array.<T>} array
     * @template T
     */
    lazyInitClasses.ArrayIterator = Kotlin.createClass(
        function () {
            return [Kotlin.kotlin.collections.MutableIterator];
        },
        /** @constructs */
        function (array) {
            this.array = array;
            this.index = 0;
        },
        /** @lends {ArrayIterator.prototype} */
        {
            next: function () {
                return this.array[this.index++];
            },
            hasNext: function () {
                return this.index < this.array.length;
            },
            remove: function () {
                if (this.index < 0 || this.index > this.array.length) throw new RangeError();
                this.index--;
                this.array.splice(this.index, 1);
            }
    });

    /**
     * @class
     * @extends {ArrayIterator.<T>}
     *
     * @constructor
     * @param {Kotlin.AbstractList.<T>} list
     * @template T
     */
    lazyInitClasses.ListIterator = Kotlin.createClass(
        function () {
            return [Kotlin.kotlin.collections.ListIterator];  // TODO: MutableListIterator
        },
        /** @constructs */
        function (list, index) {
            this.list = list;
            this.size = list.size;
            this.index = (index === undefined) ? 0 : index;
        }, {
            hasNext: function () {
                return this.index < this.size;
            },
            nextIndex: function () {
                return this.index;
            },
            next: function () {
                var index = this.index;
                var result = this.list.get_za3lpa$(index);
                this.index = index + 1;
                return result;
            },
            hasPrevious: function() {
                return this.index > 0;
            },
            previousIndex: function () {
                return this.index - 1;
            },
            previous: function () {
                var index = this.index - 1;
                var result = this.list.get_za3lpa$(index);
                this.index = index;
                return result;
            }
    });

    Kotlin.Annotation = Kotlin.createTraitNow(null);

    Kotlin.Enum = Kotlin.createClassNow(null,
        function () {
            this.name$ = void 0;
            this.ordinal$ = void 0;
        }, {
            name: {
                get: function () {
                    return this.name$;
                }
            },
            ordinal: {
                get: function () {
                    return this.ordinal$;
                }
            },
            equals_za3rmp$: function (o) {
                return this === o;
            },
            hashCode: function () {
                return getObjectHashCode(this);
            },
            compareTo_za3rmp$: function (o) {
                return this.ordinal$ < o.ordinal$ ? -1 : this.ordinal$ > o.ordinal$ ? 1 : 0;
            },
            toString: function () {
                return this.name;
            }
        }
    );

    Kotlin.RandomAccess = Kotlin.createTraitNow(null);

    Kotlin.PropertyMetadata = Kotlin.createClassNow(null,
        function (name) {
            this.name = name;
        }
    );

    lazyInitClasses.AbstractCollection = Kotlin.createClass(
        function () {
            return [Kotlin.kotlin.collections.MutableCollection];
        }, null, {
        addAll_wtfk93$: function (collection) {
            var modified = false;
            var it = collection.iterator();
            while (it.hasNext()) {
                if (this.add_za3rmp$(it.next())) {
                    modified = true;
                }
            }
            return modified
        },
        removeAll_wtfk93$: function (c) {
            var modified = false;
            var it = this.iterator();
            while (it.hasNext()) {
                if (c.contains_za3rmp$(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified
        },
        retainAll_wtfk93$: function (c) {
            var modified = false;
            var it = this.iterator();
            while (it.hasNext()) {
                if (!c.contains_za3rmp$(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified
        },
        clear: function () {
            // TODO: implement with mutable iterator
            throw new Kotlin.NotImplementedError("Not implemented yet, see KT-7809");
        },
        containsAll_wtfk93$: function (c) {
            var it = c.iterator();
            while (it.hasNext()) {
                if (!this.contains_za3rmp$(it.next())) return false;
            }
            return true;
        },
        isEmpty: function () {
            return this.size === 0;
        },
        iterator: function () {
            // TODO: Do not implement mutable iterator() this way, make abstract
            return new Kotlin.ArrayIterator(this.toArray());
        },
        equals_za3rmp$: function (o) {
            if (this.size !== o.size) return false;

            var iterator1 = this.iterator();
            var iterator2 = o.iterator();
            var i = this.size;
            while (i-- > 0) {
                if (!Kotlin.equals(iterator1.next(), iterator2.next())) {
                    return false;
                }
            }

            return true;
        },
        toString: function () {
            var builder = "[";
            var iterator = this.iterator();
            var first = true;
            var i = this.size;
            while (i-- > 0) {
                if (first) {
                    first = false;
                }
                else {
                    builder += ", ";
                }
                builder += Kotlin.toString(iterator.next());
            }
            builder += "]";
            return builder;
        },
        toJSON: function () {
            return this.toArray();
        }
    });

    /**
     * @interface // actually it's abstract class
     * @template T
     */
    lazyInitClasses.AbstractList = Kotlin.createClass(
        function () {
            return [Kotlin.kotlin.collections.MutableList, Kotlin.AbstractCollection];
        }, null, {
        iterator: function () {
            return new Kotlin.ListIterator(this);
        },
        listIterator: function() {
            return new Kotlin.ListIterator(this);
        },
        listIterator_za3lpa$: function(index) {
            if (index < 0 || index > this.size) {
                throw new Kotlin.IndexOutOfBoundsException("Index: " + index + ", size: " + this.size);
            }
            return new Kotlin.ListIterator(this, index);
        },
        add_za3rmp$: function (element) {
            this.add_vux3hl$(this.size, element);
            return true;
        },
        addAll_j97iir$: function (index, collection) {
            // TODO: implement
            throw new Kotlin.NotImplementedError("Not implemented yet, see KT-7809");
        },
        remove_za3rmp$: function (o) {
            var index = this.indexOf_za3rmp$(o);
            if (index !== -1) {
                this.removeAt_za3lpa$(index);
                return true;
            }
            return false;
        },
        clear: function () {
            // TODO: implement with remove range
            throw new Kotlin.NotImplementedError("Not implemented yet, see KT-7809");
        },
        contains_za3rmp$: function (o) {
            return this.indexOf_za3rmp$(o) !== -1;
        },
        indexOf_za3rmp$: function (o) {
            var i = this.listIterator();
            while (i.hasNext())
                if (Kotlin.equals(i.next(), o))
                    return i.previousIndex();
            return -1;
        },
        lastIndexOf_za3rmp$: function (o) {
            var i = this.listIterator_za3lpa$(this.size);
            while (i.hasPrevious())
                if (Kotlin.equals(i.previous(), o))
                    return i.nextIndex();
            return -1;
        },
        subList_vux9f0$: function(fromIndex, toIndex) {
            if (fromIndex < 0 || toIndex > this.size)
                throw new Kotlin.IndexOutOfBoundsException();
            if (fromIndex > toIndex)
                throw new Kotlin.IllegalArgumentException();
            return new Kotlin.SubList(this, fromIndex, toIndex);
        },
        hashCode: function() {
            var result = 1;
            var i = this.iterator();
            while (i.hasNext()) {
                var obj = i.next();
                result = (31*result + Kotlin.hashCode(obj)) | 0;
            }
            return result;
        }
    });

    lazyInitClasses.SubList = Kotlin.createClass(
        function () {
            return [Kotlin.AbstractList];
        },
        function (list, fromIndex, toIndex) {
            this.list = list;
            this.offset = fromIndex;
            this._size = toIndex - fromIndex;
        }, {
            get_za3lpa$: function (index) {
                this.checkRange(index);
                return this.list.get_za3lpa$(index + this.offset);
            },
            set_vux3hl$: function (index, value) {
                this.checkRange(index);
                this.list.set_vux3hl$(index + this.offset, value);
            },
            size: {
               get: function () {
                   return this._size;
               }
            },
            add_vux3hl$: function (index, element) {
                if (index < 0 || index > this.size) {
                    throw new Kotlin.IndexOutOfBoundsException();
                }
                this.list.add_vux3hl$(index + this.offset, element);
            },
            removeAt_za3lpa$: function (index) {
                this.checkRange(index);
                var result = this.list.removeAt_za3lpa$(index + this.offset);
                this._size--;
                return result;

            },
            checkRange: function (index) {
                if (index < 0 || index >= this._size) {
                    throw new Kotlin.IndexOutOfBoundsException();
                }
            }
        });

    //TODO: should be JS Array-like (https://developer.mozilla.org/en-US/docs/JavaScript/Guide/Predefined_Core_Objects#Working_with_Array-like_objects)
    lazyInitClasses.ArrayList = Kotlin.createClass(
        function () {
            return [Kotlin.AbstractList, Kotlin.RandomAccess];
        },
        function () {
            this.array = [];
        }, {
            get_za3lpa$: function (index) {
                this.checkRange(index);
                return this.array[index];
            },
            set_vux3hl$: function (index, value) {
                this.checkRange(index);
                this.array[index] = value;
            },
            size: {
               get: function () {
                   return this.array.length;
               }
            },
            iterator: function () {
                return Kotlin.arrayIterator(this.array);
            },
            add_za3rmp$: function (element) {
                this.array.push(element);
                return true;
            },
            add_vux3hl$: function (index, element) {
                this.array.splice(index, 0, element);
            },
            addAll_wtfk93$: function (collection) {
                if (collection.size == 0) {
                    return false;
                }
                var it = collection.iterator();
                for (var i = this.array.length, n = collection.size; n-- > 0;) {
                    this.array[i++] = it.next();
                }
                return true;
            },
            removeAt_za3lpa$: function (index) {
                this.checkRange(index);
                return this.array.splice(index, 1)[0];
            },
            clear: function () {
                this.array.length = 0;
            },
            indexOf_za3rmp$: function (o) {
                for (var i = 0; i < this.array.length; i++) {
                    if (Kotlin.equals(this.array[i], o)) {
                        return i;
                    }
                }
                return -1;
            },
            lastIndexOf_za3rmp$: function (o) {
                for (var i = this.array.length - 1; i >= 0; i--) {
                    if (Kotlin.equals(this.array[i], o)) {
                        return i;
                    }
                }
                return -1;
            },
            toArray: function () {
                return this.array.slice(0);
            },
            toString: function () {
                return Kotlin.arrayToString(this.array);
            },
            toJSON: function () {
                return this.array;
            },
            checkRange: function (index) {
                if (index < 0 || index >= this.array.length) {
                    throw new Kotlin.IndexOutOfBoundsException();
                }
            }
        });

    Kotlin.Runnable = Kotlin.createClassNow(null, null, {
        run: throwAbstractFunctionInvocationError("Runnable#run")
    });

    Kotlin.Comparable = Kotlin.createClassNow(null, null, {
        compareTo: throwAbstractFunctionInvocationError("Comparable#compareTo")
    });

    Kotlin.Appendable = Kotlin.createClassNow(null, null, {
        append: throwAbstractFunctionInvocationError("Appendable#append")
    });

    Kotlin.Closeable = Kotlin.createClassNow(null, null, {
        close: throwAbstractFunctionInvocationError("Closeable#close")
    });

    Kotlin.safeParseInt = function (str) {
        var r = parseInt(str, 10);
        return isNaN(r) ? null : r;
    };

    Kotlin.safeParseDouble = function (str) {
        var r = parseFloat(str);
        return isNaN(r) ? null : r;
    };

    Kotlin.arrayEquals = function (a, b) {
        if (a === b) {
            return true;
        }
        if (!Array.isArray(b) || a.length !== b.length) {
            return false;
        }

        for (var i = 0, n = a.length; i < n; i++) {
            if (!Kotlin.equals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    };

    var BaseOutput = Kotlin.createClassNow(null, null, {
            println: function (a) {
                if (typeof a !== "undefined") this.print(a);
                this.print("\n");
            },
            flush: function () {
            }
        }
    );

    Kotlin.NodeJsOutput = Kotlin.createClassNow(BaseOutput,
        function(outputStream) {
            this.outputStream = outputStream;
        }, {
            print: function (a) {
                this.outputStream.write(a);
            }
        }
    );

    Kotlin.OutputToConsoleLog = Kotlin.createClassNow(BaseOutput, null, {
            print: function (a) {
                console.log(a);
            },
            println: function (a) {
                this.print(typeof a !== "undefined" ? a : "");
            }
        }
    );

    Kotlin.BufferedOutput = Kotlin.createClassNow(BaseOutput,
        function() {
            this.buffer = ""
        }, {
            print: function (a) {
                this.buffer += String(a);
            },
            flush: function () {
                this.buffer = "";
            }
        }
    );

    Kotlin.BufferedOutputToConsoleLog = Kotlin.createClassNow(Kotlin.BufferedOutput,
        function() {
            Kotlin.BufferedOutput.call(this);
        }, {
            print: function (a) {
                var s = String(a);

                var i = s.lastIndexOf("\n");
                if (i != -1) {
                    this.buffer += s.substr(0, i);

                    this.flush();

                    s = s.substr(i + 1);
                }

                this.buffer += s;
            },
            flush: function () {
                console.log(this.buffer);
                this.buffer = "";
            }
        }
    );
    Kotlin.out = function() {
        var isNode = typeof process !== 'undefined' && process.versions && !!process.versions.node;

        if (isNode) return new Kotlin.NodeJsOutput(process.stdout);

        return new Kotlin.BufferedOutputToConsoleLog();
    }();

    Kotlin.println = function (s) {
        Kotlin.out.println(s);
    };

    Kotlin.print = function (s) {
        Kotlin.out.print(s);
    };

    lazyInitClasses.RangeIterator = Kotlin.createClass(
        function () {
            return [Kotlin.kotlin.collections.Iterator];
        },
        function (start, end, step) {
            this.start = start;
            this.end = end;
            this.step = step;
            this.i = start;
        }, {
            next: function () {
                var value = this.i;
                this.i = this.i + this.step;
                return value;
            },
            hasNext: function () {
                if (this.step > 0)
                    return this.i <= this.end;
                else
                    return this.i >= this.end;
            }
    });

    function isSameNotNullRanges(other) {
        var classObject = this.constructor;
        if (this instanceof classObject && other instanceof classObject) {
            return this.isEmpty() && other.isEmpty() ||
                (this.first === other.first && this.last === other.last && this.step === other.step);
        }
        return false;
    }

    function isSameLongRanges(other) {
        var classObject = this.constructor;
        if (this instanceof classObject && other instanceof classObject) {
            return this.isEmpty() && other.isEmpty() ||
                   (this.first.equals_za3rmp$(other.first) && this.last.equals_za3rmp$(other.last) && this.step.equals_za3rmp$(other.step));
        }
        return false;
    }

    // reference implementation in core/builtins/src/kotlin/internal/progressionUtil.kt
    function getProgressionFinalElement(start, end, step) {
        function mod(a, b) {
            var mod = a % b;
            return mod >= 0 ? mod : mod + b;
        }
        function differenceModulo(a, b, c) {
            return mod(mod(a, c) - mod(b, c), c);
        }

        if (step > 0) {
            return end - differenceModulo(end, start, step);
        }
        else if (step < 0) {
            return end + differenceModulo(start, end, -step);
        }
        else {
            throw new Kotlin.IllegalArgumentException('Step is zero.');
        }
    }

    // reference implementation in core/builtins/src/kotlin/internal/progressionUtil.kt
    function getProgressionFinalElementLong(start, end, step) {
        function mod(a, b) {
            var mod = a.modulo(b);
            return !mod.isNegative() ? mod : mod.add(b);
        }
        function differenceModulo(a, b, c) {
            return mod(mod(a, c).subtract(mod(b, c)), c);
        }

        var diff;
        if (step.compareTo_za3rmp$(Kotlin.Long.ZERO) > 0) {
            diff = differenceModulo(end, start, step);
            return diff.isZero() ? end : end.subtract(diff);
        }
        else if (step.compareTo_za3rmp$(Kotlin.Long.ZERO) < 0) {
            diff = differenceModulo(start, end, step.unaryMinus());
            return diff.isZero() ? end : end.add(diff);
        }
        else {
            throw new Kotlin.IllegalArgumentException('Step is zero.');
        }
    }

    lazyInitClasses.NumberProgression = Kotlin.createClass(
        function () {
            return [Kotlin.kotlin.collections.Iterable];
        },
        function (start, end, step) {
            this.first = start;
            this.last = getProgressionFinalElement(start, end, step);
            this.step = step;
            if (this.step === 0)
                throw new Kotlin.IllegalArgumentException('Step must be non-zero');
        }, {
            iterator: function () {
                return new Kotlin.RangeIterator(this.first, this.last, this.step);
            },
            isEmpty: function () {
                return this.step > 0 ? this.first > this.last : this.first < this.last;
            },
            hashCode: function () {
                return this.isEmpty() ? -1 : 31 * (31 * this.first + this.last) + this.step;
            },
            equals_za3rmp$: isSameNotNullRanges,
            toString: function () {
                return this.step > 0 ? this.first.toString() + '..' + this.last + ' step ' + this.step : this.first.toString() + ' downTo ' + this.last + ' step ' + -this.step;
            }
        });

    lazyInitClasses.NumberRange = Kotlin.createClass(
        function() {
            return [Kotlin.kotlin.ranges.ClosedRange, Kotlin.NumberProgression]
        },
        function $fun(start, endInclusive) {
            $fun.baseInitializer.call(this, start, endInclusive, 1);
            this.start = start;
            this.endInclusive = endInclusive;
        }, {
            contains_htax2k$: function (item) {
                return this.start <= item && item <= this.endInclusive;
            },
            isEmpty: function () {
                return this.start > this.endInclusive;
            },
            hashCode: function () {
                return this.isEmpty() ? -1 : 31 * this.start + this.endInclusive;
            },
            equals_za3rmp$: isSameNotNullRanges,
            toString: function () {
                return this.start.toString() + '..' + this.endInclusive;
            }
        }, {
            Companion: Kotlin.createObject(null, function () {
                this.EMPTY = new Kotlin.NumberRange(1, 0);
            })
        });



    lazyInitClasses.LongRangeIterator = Kotlin.createClass(
        function () {
            return [Kotlin.kotlin.collections.Iterator];
        },
         function (start, end, step) {
             this.start = start;
             this.end = end;
             this.step = step;
             this.i = start;
         }, {
             next: function () {
                 var value = this.i;
                 this.i = this.i.add(this.step);
                 return value;
             },
             hasNext: function () {
                 if (this.step.isNegative())
                     return this.i.compare(this.end) >= 0;
                 else
                     return this.i.compare(this.end) <= 0;
             }
         });

    lazyInitClasses.LongProgression = Kotlin.createClass(
        function () {
            return [Kotlin.kotlin.collections.Iterable];
        },
        function (start, end, step) {
            this.first = start;
            this.last = getProgressionFinalElementLong(start, end, step);
            this.step = step;
            if (this.step.isZero())
                throw new Kotlin.IllegalArgumentException('Step must be non-zero');
        }, {
            iterator: function () {
                return new Kotlin.LongRangeIterator(this.first, this.last, this.step);
            },
            isEmpty: function() {
                return this.step.isNegative() ? this.first.compare(this.last) < 0 : this.first.compare(this.last) > 0;
            },
            hashCode: function() {
                return this.isEmpty() ? -1 : (31 * (31 * this.first.toInt() + this.last.toInt()) + this.step.toInt());
            },
            equals_za3rmp$: isSameLongRanges,
            toString: function () {
                return !this.step.isNegative() ? this.first.toString() + '..' + this.last + ' step ' + this.step : this.first.toString() + ' downTo ' + this.last + ' step ' + this.step.unaryMinus();
            }
        });

    lazyInitClasses.LongRange = Kotlin.createClass(
        function () {
            return [Kotlin.kotlin.ranges.ClosedRange, Kotlin.LongProgression];
        },
        function $fun(start, endInclusive) {
            $fun.baseInitializer.call(this, start, endInclusive, Kotlin.Long.ONE);
            this.start = start;
            this.endInclusive = endInclusive;
        }, {
            contains_htax2k$: function (item) {
                return this.start.compareTo_za3rmp$(item) <= 0 && item.compareTo_za3rmp$(this.endInclusive) <= 0;
            },
            isEmpty: function () {
                return this.start.compare(this.endInclusive) > 0;
            },
            hashCode: function() {
                return this.isEmpty() ? -1 : (31 * this.start.toInt() + this.endInclusive.toInt());
            },
            equals_za3rmp$: isSameLongRanges,
            toString: function () {
                return this.start.toString() + '..' + this.endInclusive;
            }
        }, {
            Companion: Kotlin.createObject(null, function () {
                this.EMPTY = new Kotlin.LongRange(Kotlin.Long.ONE, Kotlin.Long.ZERO);
            })
        });



    lazyInitClasses.CharRangeIterator = Kotlin.createClass(
        function () {
            return [Kotlin.RangeIterator];
        },
        function (start, end, step) {
            Kotlin.RangeIterator.call(this, start, end, step);
        }, {
            next: function () {
                var value = this.i;
                this.i = this.i + this.step;
                return String.fromCharCode(value);
            }
    });

    lazyInitClasses.CharProgression = Kotlin.createClassNow(
        function () {
            return [Kotlin.kotlin.collections.Iterable];
        },
        function (start, end, step) {
            this.first = start;
            this.startCode = start.charCodeAt(0);
            this.endCode = getProgressionFinalElement(this.startCode, end.charCodeAt(0), step);
            this.last = String.fromCharCode(this.endCode);
            this.step = step;
            if (this.step === 0)
                throw new Kotlin.IllegalArgumentException('Increment must be non-zero');
        }, {
            iterator: function () {
                return new Kotlin.CharRangeIterator(this.startCode, this.endCode, this.step);
            },
            isEmpty: function() {
                return this.step > 0 ? this.startCode > this.endCode : this.startCode < this.endCode;
            },
            hashCode: function() {
                return this.isEmpty() ? -1 : (31 * (31 * this.startCode|0 + this.endCode|0) + this.step|0);
            },
            equals_za3rmp$: isSameNotNullRanges,
            toString: function () {
                return this.step > 0 ? this.first.toString() + '..' + this.last + ' step ' + this.step : this.first.toString() + ' downTo ' + this.last + ' step ' + -this.step;
            }
    });


    lazyInitClasses.CharRange = Kotlin.createClass(
        function() {
            return [Kotlin.kotlin.ranges.ClosedRange, Kotlin.CharProgression]
        },
        function $fun(start, endInclusive) {
            $fun.baseInitializer.call(this, start, endInclusive, 1);
            this.start = start;
            this.endInclusive = endInclusive;
        }, {
            contains_htax2k$: function (item) {
                return this.start <= item && item <= this.endInclusive;
            },
            isEmpty: function () {
                return this.start > this.endInclusive;
            },
            hashCode: function() {
                return this.isEmpty() ? -1 : (31 * this.startCode|0 + this.endCode|0);
            },
            equals_za3rmp$: isSameNotNullRanges,
            toString: function () {
                return this.start.toString() + '..' + this.endInclusive;
            }
        }, {
            Companion: Kotlin.createObject(null, function () {
                this.EMPTY = new Kotlin.CharRange(Kotlin.toChar(1), Kotlin.toChar(0));
            })
        });


    /**
     * @interface
     * @template T
     */
    Kotlin.Comparator = Kotlin.createClassNow(null, null, {
        compare: throwAbstractFunctionInvocationError("Comparator#compare")
    });

    Kotlin.collectionsMax = function (c, comp) {
        if (c.isEmpty()) {
            //TODO: which exception?
            throw new Error();
        }
        var it = c.iterator();
        var max = it.next();
        while (it.hasNext()) {
            var el = it.next();
            if (comp.compare(max, el) < 0) {
                max = el;
            }
        }
        return max;
    };

    Kotlin.collectionsSort = function (mutableList, comparator) {
        var boundComparator = void 0;
        if (comparator !== void 0) {
            boundComparator = comparator.compare.bind(comparator);
        }

        if (mutableList instanceof Array) {
            mutableList.sort(boundComparator);
        }

        if (mutableList.size > 1) {
            //TODO: should be deleted when List will be JS Array-like (https://developer.mozilla.org/en-US/docs/JavaScript/Guide/Predefined_Core_Objects#Working_with_Array-like_objects)
            var array = Kotlin.copyToArray(mutableList);

            array.sort(boundComparator);

            for (var i = 0, n = array.length; i < n; i++) {
                mutableList.set_vux3hl$(i, array[i]);
            }
        }
    };

    Kotlin.primitiveArraySort = function(array) {
        array.sort(Kotlin.primitiveCompareTo)
    };

    Kotlin.copyToArray = function (collection) {
        if (typeof collection.toArray !== "undefined") return collection.toArray();

        var array = [];
        var it = collection.iterator();
        while (it.hasNext()) {
            array.push(it.next());
        }

        return array;
    };


    Kotlin.StringBuilder = Kotlin.createClassNow([CharSequence],
        function (content) {
            this.string = typeof(content) == "string" ? content : "";
        }, {
        length: {
            get: function() {
                return this.string.length;
            }
        },
        substring: function(start, end) {
            return this.string.substring(start, end);
        },
        charAt: function(index) {
            return this.string.charAt(index);
        },
        append: function (obj, from, to) {
            if (from == void 0 && to == void 0) {
                this.string = this.string + obj.toString();
            } else if (to == void 0) {
                this.string = this.string + obj.toString().substring(from);
            } else {
                this.string = this.string + obj.toString().substring(from, to);
            }

            return this;
        },
        reverse: function () {
            this.string = this.string.split("").reverse().join("");
            return this;
        },
        toString: function () {
            return this.string;
        }
    });

    Kotlin.splitString = function (str, regex, limit) {
        return str.split(new RegExp(regex), limit);
    };

    Kotlin.nullArray = function (size) {
        var res = [];
        var i = size;
        while (i > 0) {
            res[--i] = null;
        }
        return res;
    };

    Kotlin.numberArrayOfSize = function (size) {
        return Kotlin.arrayFromFun(size, function () {
            return 0;
        });
    };

    Kotlin.charArrayOfSize = function (size) {
        return Kotlin.arrayFromFun(size, function () {
            return '\0';
        });
    };

    Kotlin.booleanArrayOfSize = function (size) {
        return Kotlin.arrayFromFun(size, function () {
            return false;
        });
    };

    Kotlin.longArrayOfSize = function (size) {
        return Kotlin.arrayFromFun(size, function () {
            return Kotlin.Long.ZERO;
        });
    };

    Kotlin.arrayFromFun = function (size, initFun) {
        var result = new Array(size);
        for (var i = 0; i < size; i++) {
            result[i] = initFun(i);
        }
        return result;
    };

    Kotlin.arrayIterator = function (array) {
        return new Kotlin.ArrayIterator(array);
    };

    Kotlin.jsonAddProperties = function (obj1, obj2) {
        for (var p in obj2) {
            if (obj2.hasOwnProperty(p)) {
                obj1[p] = obj2[p];
            }
        }
        return obj1;
    };
    
    Kotlin.lazyInitClasses = Kotlin.lazyInitClasses || [];
    Kotlin.lazyInitClasses.push(lazyInitClasses);
})(Kotlin);

