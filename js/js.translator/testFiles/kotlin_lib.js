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

// todo inlined
String.prototype.startsWith = function (s) {
  return this.indexOf(s) === 0;
};

String.prototype.endsWith = function (s) {
  return this.indexOf(s, this.length - s.length) !== -1;
};

String.prototype.contains = function (s) {
  return this.indexOf(s) !== -1;
};

// todo org.jetbrains.k2js.test.semantics.WebDemoExamples2Test#testBuilder
var kotlin = {set:function (receiver, key, value) {
    return receiver.put(key, value);
}};

(function () {
    Kotlin.equals = function (obj1, obj2) {
        if (obj1 === null || obj1 === undefined) {
            return obj2 === null;
        }

        if (obj1 instanceof Array) {
            if (!(obj2 instanceof Array) || obj1.length != obj2.length) {
                return false;
            }
            for (var i = 0; i < obj1.length; i++) {
                if (!Kotlin.equals(obj1[i], obj2[i])) {
                    return false;
                }
            }
            return true;
        }

        if (typeof obj1 == "object" && obj1.equals !== undefined) {
            return obj1.equals(obj2);
        }

        return obj1 === obj2;
    };

    Kotlin.intUpto = function (from, limit) {
        return Kotlin.$new(Kotlin.NumberRange)(from, limit - from + 1, false);
    };

    Kotlin.intDownto = function (from, limit) {
        return Kotlin.$new(Kotlin.NumberRange)(from, from - limit + 1, true);
    };

    Kotlin.modules = {};

    Kotlin.Exception = Kotlin.$createClass();
    Kotlin.RuntimeException = Kotlin.$createClass(Kotlin.Exception);
    Kotlin.IndexOutOfBounds = Kotlin.$createClass(Kotlin.Exception);
    Kotlin.NullPointerException = Kotlin.$createClass(Kotlin.Exception);
    Kotlin.NoSuchElementException = Kotlin.$createClass(Kotlin.Exception);
    Kotlin.IllegalArgumentException = Kotlin.$createClass(Kotlin.Exception);
    Kotlin.IllegalStateException = Kotlin.$createClass(Kotlin.Exception);
    Kotlin.IndexOutOfBoundsException = Kotlin.$createClass(Kotlin.Exception);
    Kotlin.UnsupportedOperationException = Kotlin.$createClass(Kotlin.Exception);
    Kotlin.IOException = Kotlin.$createClass(Kotlin.Exception);

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

    var ListIterator = Kotlin.$createClass(ArrayIterator, {
        initialize: function (list) {
            this.list = list;
            this.size = list.size();
            this.index = 0;
        },
        next: function () {
            return this.list.get(this.index++);
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

    Kotlin.AbstractList = Kotlin.$createClass(Kotlin.AbstractCollection, {
        iterator: function () {
            return Kotlin.$new(ListIterator)(this);
        },
        remove: function (o) {
            var index = this.indexOf(o);
            if (index !== -1) {
                this.removeAt(index);
            }
        },
        contains: function (o) {
            return this.indexOf(o) !== -1;
        }
    });

    Kotlin.ArrayList = Kotlin.$createClass(Kotlin.AbstractList, {
        initialize: function () {
            this.array = [];
            this.$size = 0;
        },
        get: function (index) {
            if (index < 0 || index >= this.$size) {
                throw Kotlin.IndexOutOfBounds;
            }
            return this.array[index];
        },
        set: function (index, value) {
            if (index < 0 || index >= this.$size) {
                throw Kotlin.IndexOutOfBounds;
            }
            this.array[index] = value;
        },
        size: function () {
            return this.$size;
        },
        iterator: function () {
            return Kotlin.arrayIterator(this.array);
        },
        add: function (element) {
            this.array[this.$size++] = element;
        },
        addAt: function (index, element) {
            this.array.splice(index, 0, element);
            this.$size++;
        },
        addAll: function (collection) {
            var it = collection.iterator();
            for (var i = this.$size, n = collection.size(); n-- > 0;) {
                this.array[i++] = it.next();
            }

            this.$size += collection.size();
        },
        removeAt: function (index) {
            this.array.splice(index, 1);
            this.$size--;
        },
        clear: function () {
            this.array.length = 0;
            this.$size = 0;
        },
        indexOf: function (o) {
            for (var i = 0, n = this.$size; i < n; ++i) {
                if (Kotlin.equals(this.array[i], o)) {
                    return i;
                }
            }
            return -1;
        },
        toArray: function () {
            return this.array.slice(0, this.$size);
        },
        toString: function () {
            return "[" + this.array.join(", ") + "]";
        },
        toJSON: function () {
            return this.array;
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

    Kotlin.collectionsMax = function (col, comp) {
        var it = col.iterator();
        if (col.isEmpty()) {
            //TODO: which exception?
            throw Kotlin.Exception();
        }
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

    Kotlin.splitString = function (str, regex) {
        return str.split(regex);
    };

    Kotlin.nullArray = function (size) {
        var res = [];
        var i = size;
        while (i > 0) {
            res[--i] = null;
        }
        return res;
    };

    Kotlin.arrayFromFun = function (size, initFun) {
        var result = new Array(size);
        for (var i = 0; i < size; i++) {
            result[i] = initFun(i);
        }
        return result;
    };

    Kotlin.arrayIndices = function (arr) {
        return Kotlin.$new(Kotlin.NumberRange)(0, arr.length);
    };

    Kotlin.arrayIterator = function (array) {
        return Kotlin.$new(ArrayIterator)(array);
    };

    Kotlin.toString = function (obj) {
        return obj.toString();
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

    //TODO: use intrinsic
    Kotlin.sure = function (obj) {
        return obj;
    };
})();

Kotlin.assignOwner = function(f, o) {
  f.o = o;
  return f;
};