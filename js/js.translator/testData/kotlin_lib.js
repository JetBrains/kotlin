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
    Kotlin.ConcurrentModificationException = createClassNowWithMessage(Kotlin.RuntimeException);
    Kotlin.ClassCastException = createClassNowWithMessage(Kotlin.RuntimeException);
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

    lazyInitClasses.Enum = Kotlin.createClass(
        function() {
            return [Kotlin.Comparable];
        },
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

    Kotlin.PropertyMetadata = Kotlin.createClassNow(null,
        function (name) {
            this.name = name;
        }
    );


    Kotlin.Comparable = Kotlin.createTraitNow(null, null, {
        compareTo: throwAbstractFunctionInvocationError("Comparable#compareTo")
    });

    Kotlin.Appendable = Kotlin.createTraitNow(null, null, {
        append: throwAbstractFunctionInvocationError("Appendable#append")
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

    Kotlin.collectionsSort = function (mutableList, comparator) {
        var boundComparator = void 0;
        if (comparator !== void 0) {
            boundComparator = comparator.compare.bind(comparator);
        }

        if (mutableList.size > 1) {
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
        return Kotlin.copyToArrayImpl(collection);
    };

    Kotlin.copyToArrayImpl = function (collection) {
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

    Kotlin.deleteProperty = function (object, property) {
        delete object[property];
    };

    Kotlin.jsonAddProperties = function (obj1, obj2) {
        for (var p in obj2) {
            if (obj2.hasOwnProperty(p)) {
                obj1[p] = obj2[p];
            }
        }
        return obj1;
    };

    Kotlin.createDefinition(lazyInitClasses, Kotlin);
})(Kotlin);

