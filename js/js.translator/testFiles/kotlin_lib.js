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

"use strict";

(function () {
    Kotlin.equals = function (obj1, obj2) {
        if (obj1 === null || obj1 === undefined) {
            return obj2 === null || obj2 === undefined;
        }

        if (Array.isArray(obj1)) {
            return Kotlin.arrayEquals(obj1, obj2);
        }

        if (typeof obj1 == "object" && obj1.equals !== undefined) {
            return obj1.equals(obj2);
        }

        return obj1 === obj2;
    };
    Kotlin.stringify = function (o) {
        if (o === null || o === undefined) {
            return "null";
        }
        else if (Array.isArray(o)) {
            return Kotlin.arrayToString(o);
        }
        else {
            return o.toString();
        }
    };
    Kotlin.arrayToString = function(a) {
        return "[" + a.join(", ") + "]";
    };

    Kotlin.intUpto = function (from, limit) {
        return Kotlin.$new(Kotlin.NumberRange)(from, limit - from + 1, false);
    };

    Kotlin.intDownto = function (from, limit) {
        return Kotlin.$new(Kotlin.NumberRange)(from, from - limit + 1, true);
    };

    Kotlin.modules = {};

    Kotlin.RuntimeException = Kotlin.$createClass();
    Kotlin.NullPointerException = Kotlin.$createClass();
    Kotlin.NoSuchElementException = Kotlin.$createClass();
    Kotlin.IllegalArgumentException = Kotlin.$createClass();
    Kotlin.IllegalStateException = Kotlin.$createClass();
    Kotlin.UnsupportedOperationException = Kotlin.$createClass();
    Kotlin.IOException = Kotlin.$createClass();

    Kotlin.throwNPE = function () {
        throw Kotlin.$new(Kotlin.NullPointerException)();
    };

    function throwAbstractFunctionInvocationError(funName) {
        return function() {
            var message;
            if (funName !== undefined) {
                message = "Function " + funName + " is abstract";
            } else {
                message = "Function is abstract";
            }
            throw new TypeError(message);
        };
    }

    Kotlin.Iterator = Kotlin.$createClass({
        initialize: function () {
        },
        next: throwAbstractFunctionInvocationError("Iterator#next"),
        hasNext: throwAbstractFunctionInvocationError("Iterator#hasNext")
    });

    var ArrayIterator = Kotlin.$createClass(Kotlin.Iterator, {
        initialize: function (array) {
            this.array = array;
            this.size = array.length;
            this.index = 0;
        },
        next: function () {
            return this.array[this.index++];
        },
        hasNext: function () {
            return this.index < this.size;
        }
    });

    Kotlin.Collection = Kotlin.$createClass();

    Kotlin.AbstractCollection = Kotlin.$createClass(Kotlin.Collection, {
        size: function () {
            return this.$size;
        },
        addAll: function (collection) {
            var it = collection.iterator();
            var i = this.size();
            while (i-- > 0) {
                this.add(it.next());
            }
        },
        isEmpty: function () {
            return this.size() === 0;
        },
        iterator: function () {
            return Kotlin.$new(ArrayIterator)(this.toArray());
        },
        equals: function (o) {
            if (this.size() === o.size()) {
                var iterator1 = this.iterator();
                var iterator2 = o.iterator();
                var i = this.size();
                while (i-- > 0) {
                    if (!Kotlin.equals(iterator1.next(), iterator2.next())) {
                        return false;
                    }
                }
            }
            return true;
        },
        toString: function () {
            var builder = "[";
            var iterator = this.iterator();
            var first = true;
            var i = this.$size;
            while (i-- > 0) {
                if (first) {
                    first = false;
                }
                else {
                    builder += ", ";
                }
                builder += iterator.next();
            }
            builder += "]";
            return builder;
        },
        toJSON: function () {
            return this.toArray();
        }
    });

    Kotlin.Runnable = Kotlin.$createClass({
        initialize: function () {
        },
        run: throwAbstractFunctionInvocationError("Runnable#run")
    });

    Kotlin.Comparable = Kotlin.$createClass({
        initialize: function () {
        },
        compareTo: throwAbstractFunctionInvocationError("Comparable#compareTo")
    });

    Kotlin.Appendable = Kotlin.$createClass({
        initialize: function () {
        },
        append: throwAbstractFunctionInvocationError("Appendable#append")
    });

    Kotlin.Closeable = Kotlin.$createClass({
        initialize: function () {
        },
        close: throwAbstractFunctionInvocationError("Closeable#close")
    });

    Kotlin.parseInt = function (str) {
        return parseInt(str, 10);
    };

    Kotlin.safeParseInt = function(str) {
        var r = parseInt(str, 10);
        return isNaN(r) ? null : r;
    };

    Kotlin.safeParseDouble = function(str) {
        var r = parseFloat(str);
        return isNaN(r) ? null : r;
    };

    function rangeCheck(index, n) {
        if (index < 0 || index >= n) {
            throw new RangeError("Index: " + index + ", Size: " + n);
        }
    }

    Kotlin.collectionAdd = function (c, o) {
        return Array.isArray(c) ? c.push(o) : c.add(o);
    };
    Kotlin.collectionAddAll = function (c, collection) {
        if (Array.isArray(c)) {
            return Kotlin.arrayAddAll(c, collection);
        }

        return c.addAll(collection);
    };
    Kotlin.collectionRemove = function (c, o) {
        return Array.isArray(c) ? Kotlin.arrayRemove(c, o) : c.remove(o);
    };
    Kotlin.collectionClear = function (c) {
        if (Array.isArray(c)) {
            c.length = 0;
        }
        else {
            c.clear();
        }
    };
    Kotlin.collectionIterator = function (c) {
        return Array.isArray(c) ? Kotlin.arrayIterator(c) : c.iterator();
    };
    Kotlin.collectionSize = function (c) {
        return Array.isArray(c) ? c.length : c.size();
    };
    Kotlin.collectionIsEmpty = function (c) {
        return Array.isArray(c) ? c.length === 0 : c.isEmpty();
    };
    Kotlin.collectionContains = function (c, o) {
        return Array.isArray(c) ? Kotlin.arrayIndexOf(c, o) !== -1 : c.contains(o);
    };

    Kotlin.arrayIndexOf = function (a, o) {
        for (var i = 0, n = a.length; i < n; i++) {
            if (Kotlin.equals(a[i], o)) {
                return i;
            }
        }
        return -1;
    };
    Kotlin.arrayLastIndexOf = function (a, o) {
        for (var i = a.length - 1; i > -1; i--) {
            if (Kotlin.equals(a[i], o)) {
                return i;
            }
        }
        return -1;
    };
    Kotlin.arrayLastIndexOf = function (a, o) {
        for (var i = a.length - 1; i > -1; i--) {
            if (Kotlin.equals(a[i], o)) {
                return i;
            }
        }
        return -1;
    };
    Kotlin.arrayAddAll = function (a, collection) {
        var i, n;
        if (Array.isArray(collection)) {
            var j = 0;
            for (i = a.length, n = collection.length; n-- > 0;) {
                a[i++] = collection[j++];
            }
            return j > 0;
        }

        var it = collection.iterator();
        // http://jsperf.com/arrays-push-vs-index
        for (i = a.length, n = collection.size(); n-- > 0;) {
            a[i++] = it.next();
        }
        return collection.size() != 0
    };
    Kotlin.arrayAddAt = function (a, index, o) {
        if (index > a.length || index < 0) {
            throw new RangeError("Index: " + index + ", Size: " + a.length);
        }
        return a.splice(index, 0, o);
    };
    Kotlin.arrayGet = function (a, index) {
        rangeCheck(index, a.length);
        return a[index];
    };
    Kotlin.arraySet = function (a, index, o) {
        rangeCheck(index, a.length);
        a[index] = o;
        return true;
    };
    Kotlin.arrayRemoveAt = function (a, index) {
        rangeCheck(index, a.length);
        return a.splice(index, 1)[0];
    };
    Kotlin.arrayRemove = function (a, o) {
        var index = Kotlin.arrayIndexOf(a, o);
        if (index !== -1) {
            a.splice(index, 1);
            return true;
        }
        return false;
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

    Kotlin.System = function () {
        var output = "";

        var print = function (obj) {
            if (obj !== undefined) {
                if (obj === null || typeof obj !== "object") {
                    output += obj;
                }
                else {
                    output += obj.toString();
                }
            }
        };
        var println = function (obj) {
            this.print(obj);
            output += "\n";
        };

        return {
            out: function () {
                return {
                    print: print,
                    println: println
                };
            },
            output: function () {
                return output;
            },
            flush: function () {
                output = "";
            }
        };
    }();

    Kotlin.println = function (s) {
        Kotlin.System.out().println(s);
    };

    Kotlin.print = function (s) {
        Kotlin.System.out().print(s);
    };

    Kotlin.RangeIterator = Kotlin.$createClass(Kotlin.Iterator, {
        initialize: function (start, count, reversed) {
            this.$start = start;
            this.$count = count;
            this.$reversed = reversed;
            this.$i = this.get_start();
        },
        get_start: function () {
            return this.$start;
        },
        get_count: function () {
            return this.$count;
        },
        set_count: function (tmp$0) {
            this.$count = tmp$0;
        },
        get_reversed: function () {
            return this.$reversed;
        },
        get_i: function () {
            return this.$i;
        },
        set_i: function (tmp$0) {
            this.$i = tmp$0;
        },
        next: function () {
            this.set_count(this.get_count() - 1);
            if (this.get_reversed()) {
                this.set_i(this.get_i() - 1);
                return this.get_i() + 1;
            }
            else {
                this.set_i(this.get_i() + 1);
                return this.get_i() - 1;
            }
        },
        hasNext: function () {
            return this.get_count() > 0;
        }
    });

    Kotlin.NumberRange = Kotlin.$createClass({
        initialize: function (start, size, reversed) {
            this.$start = start;
            this.$size = size;
            this.$reversed = reversed;
        },
        get_start: function () {
            return this.$start;
        },
        get_size: function () {
            return this.$size;
        },
        get_reversed: function () {
            return this.$reversed;
        },
        get_end: function () {
            return this.get_reversed() ? this.get_start() - this.get_size() + 1 : this.get_start() + this.get_size() - 1;
        },
        contains: function (number) {
            if (this.get_reversed()) {
                return number <= this.get_start() && number > this.get_start() - this.get_size();
            }
            else {
                return number >= this.get_start() && number < this.get_start() + this.get_size();
            }
        },
        iterator: function () {
            return Kotlin.$new(Kotlin.RangeIterator)(this.get_start(), this.get_size(), this.get_reversed());
        }
    });

    Kotlin.Comparator = Kotlin.$createClass({
        initialize: function () {
        },
        compare: throwAbstractFunctionInvocationError("Comparator#compare")
    });

    var ComparatorImpl = Kotlin.$createClass(Kotlin.Comparator, {
        initialize: function (comparator) {
            this.compare = comparator;
        }
    });

    Kotlin.comparator = function (f) {
        return Kotlin.$new(ComparatorImpl)(f);
    };

    Kotlin.collectionsMax = function (c, comp) {
        if (Kotlin.collectionIsEmpty(c)) {
            //TODO: which exception?
            throw new Error();
        }

        var it = Kotlin.collectionIterator(c);
        var max = it.next();
        while (it.hasNext()) {
            var el = it.next();
            if (comp.compare(max, el) < 0) {
                max = el;
            }
        }
        return max;
    };

    Kotlin.StringBuilder = Kotlin.$createClass(
            {
                initialize:function () {
                    this.string = "";
                },
                append:function (obj) {
                    this.string = this.string + obj.toString();
                },
                toString:function () {
                    return this.string;
                }
            }
    );

    function nullFun(i) {
        return null;
    }

    Kotlin.arrayOfNulls = function (size) {
        return Kotlin.arrayFromFun(size, nullFun);
    };

    Kotlin.arrayFromFun = function (size, initFun) {
        var result = new Array(size);
        for (var i = 0; i < size; i++) {
            result[i] = initFun(i);
        }
        return result;
    };

    Kotlin.arrayIndices = function (array) {
        return Kotlin.$new(Kotlin.NumberRange)(0, array.length);
    };

    Kotlin.arrayIterator = function (array) {
        return Kotlin.$new(ArrayIterator)(array);
    };

    Kotlin.jsonFromTuples = function (pairArr) {
        var i = pairArr.length;
        var res = {};
        while (i > 0) {
            --i;
            res[pairArr[i][0]] = pairArr[i][1];
        }
        return res;
    };

    //TODO: use intrinsic
    Kotlin.jsonSet = function (obj, attrName, value) {
        obj[attrName] = value;
    };

    Kotlin.jsonGet = function (obj, attrName) {
        return obj[attrName];
    };


    Kotlin.jsonAddProperties = function (obj1, obj2) {
        for (var p in obj2) {
            if (obj2.hasOwnProperty(p)) {
                obj1[p] = obj2[p];
            }
        }
        return obj1;
    };
})();

Kotlin.assignOwner = function(f, o) {
  f.o = o;
  return f;
};