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

    Kotlin.array = function (args) {
        return args === null || args === undefined ? [] : args.slice();
    };

    Kotlin.intUpto = function (from, to) {
        return Kotlin.$new(Kotlin.NumberRange)(from, to);
    };

    Kotlin.intDownto = function (from, to) {
        return Kotlin.$new(Kotlin.Progression)(from, to, -1);
    };

    Kotlin.modules = {};

    Kotlin.Exception = Kotlin.$createClass();
    Kotlin.RuntimeException = Kotlin.$createClass(Kotlin.Exception);
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
        get_hasNext: function () {
            return this.index < this.size;
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
        },
        get_hasNext: function () {
            return this.index < this.size;
        }
    });

    Kotlin.Collection = Kotlin.$createClass();

    Kotlin.AbstractList = Kotlin.$createClass(Kotlin.Collection, {
        iterator: function () {
            return Kotlin.$new(ListIterator)(this);
        },
        isEmpty: function () {
            return this.size() === 0;
        },
        addAll: function (collection) {
            var it = collection.iterator();
            var i = this.$size;
            while (i-- > 0) {
                this.add(it.next());
            }
        },
        remove: function (o) {
            var index = this.indexOf(o);
            if (index !== -1) {
                this.removeAt(index);
            }
        },
        contains: function (o) {
            return this.indexOf(o) !== -1;
        },
        equals: function (o) {
            if (this.$size === o.$size) {
                var iterator1 = this.iterator();
                var iterator2 = o.iterator();
                var i = this.$size;
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

    //TODO: should be JS Array-like (https://developer.mozilla.org/en-US/docs/JavaScript/Guide/Predefined_Core_Objects#Working_with_Array-like_objects)
    Kotlin.ArrayList = Kotlin.$createClass(Kotlin.AbstractList, {
        initialize: function () {
            this.array = [];
            this.$size = 0;
        },
        get: function (index) {
            this.checkRange(index);
            return this.array[index];
        },
        set: function (index, value) {
            this.checkRange(index);
            this.array[index] = value;
        },
        toArray: function () {
            return this.array.slice(0, this.$size);
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
            this.checkRange(index);
            this.$size--;
            return this.array.splice(index, 1)[0];
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
        toString: function () {
            return "[" + this.array.join(", ") + "]";
        },
        toJSON: function () {
            return this.array;
        },
        checkRange: function(index) {
            if (index < 0 || index >= this.$size) {
                throw new Kotlin.IndexOutOfBoundsException();
            }
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
        initialize: function (start, end, increment) {
            this.$start = start;
            this.$end = end;
            this.$increment = increment;
            this.$i = start;
        },
        get_start: function () {
            return this.$start;
        },
        get_end: function () {
            return this.$end;
        },
        get_i: function () {
            return this.$i;
        },
        set_i: function (tmp$0) {
            this.$i = tmp$0;
        },
        next: function () {
            var value = this.$i;
            this.set_i(this.$i + this.$increment);
            return value;
        },
        get_hasNext: function () {
            return this.$increment > 0 ? this.$next <= this.$end : this.$next >= this.$end;
        }
    });

    Kotlin.NumberRange = Kotlin.$createClass({
        initialize: function (start, end) {
            this.$start = start;
            this.$end = end;
        },
        get_start: function () {
            return this.$start;
        },
        get_end: function () {
            return this.$end;
        },
        get_increment: function () {
            return 1;
        },
        contains: function (number) {
            return this.$start <= number && number <= this.$end;
        },
        iterator: function () {
            return Kotlin.$new(Kotlin.RangeIterator)(this.get_start(), this.get_end());
        }
    });

    Kotlin.Progression = Kotlin.$createClass({
        initialize: function (start, end, increment) {
            this.$start = start;
            this.$end = end;
            this.$increment = increment;
        },
        get_start: function () {
            return this.$start;
        },
        get_end: function () {
            return this.$end;
        },
        get_increment: function () {
            return this.$increment;
        },
        iterator: function () {
            return Kotlin.$new(Kotlin.RangeIterator)(this.get_start(), this.get_end(), this.get_increment());
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
        while (it.get_hasNext()) {
            var el = it.next();
            if (comp.compare(max, el) < 0) {
                max = el;
            }
        }
        return max;
    };

    Kotlin.collectionsSort = function (mutableList, comparator) {
        var boundComparator = undefined;
        if (comparator !== undefined) {
            boundComparator = comparator.compare.bind(comparator);
        }

        if (mutableList instanceof Array) {
            mutableList.sort(boundComparator);
        }

        //TODO: should be deleted when List will be JS Array-like (https://developer.mozilla.org/en-US/docs/JavaScript/Guide/Predefined_Core_Objects#Working_with_Array-like_objects)
        var array = [];
        var it = mutableList.iterator();
        while (it.hasNext()) {
            array.push(it.next());
        }

        array.sort(boundComparator);

        for (var i = 0, n = array.length; i < n; i++) {
            mutableList.set(i, array[i]);
        }
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

    Kotlin.numberArrayOfSize = function (size) {
        return Kotlin.arrayFromFun(size, function(){ return 0; });
    };

    Kotlin.charArrayOfSize = function (size) {
        return Kotlin.arrayFromFun(size, function(){ return '\0'; });
    };

    Kotlin.booleanArrayOfSize = function (size) {
        return Kotlin.arrayFromFun(size, function(){ return false; });
    };

    Kotlin.arrayFromFun = function (size, initFun) {
        var result = new Array(size);
        for (var i = 0; i < size; i++) {
            result[i] = initFun(i);
        }
        return result;
    };

    Kotlin.arrayIndices = function (arr) {
        return Kotlin.$new(Kotlin.NumberRange)(0, arr.length - 1);
    };

    Kotlin.arrayIterator = function (array) {
        return Kotlin.$new(ArrayIterator)(array);
    };

    Kotlin.toString = function (obj) {
        return obj.toString();
    };

    Kotlin.jsonFromPairs = function (pairArr) {
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

    // native concat doesn't work for arguments
    Kotlin.concat = function (a, b) {
        var r = new Array(a.length + b.length);
        var i = 0;
        var n = a.length;
        for (; i < n; i++) {
            r[i] = a[i];
        }
        n = b.length;
        for (var j = 0; j < n;) {
            r[i++] = b[j++];
        }
        return r;
    }
})();

Kotlin.assignOwner = function(f, o) {
  f.o = o;
  return f;
};

// we cannot use Function.bind, because if we bind with null self, but call with not null — fun must receive passed not null self
// test case: WebDemoExamples2Test.testBuilder
Kotlin.b0 = function (f, self, value) {
    return function () {
        return f.call(self !== null ? self : this, value);
    }
};
Kotlin.b1 = function (f, self, values) {
    return function () {
        return f.apply(self !== null ? self : this, values);
    }
};
Kotlin.b2 = function (f, self, values) {
    return function () {
        return f.apply(self !== null ? self : this, Kotlin.concat(values, arguments));
    }
};
Kotlin.b3 = function (f, self) {
    return function () {
        return f.call(self)
    }
};
Kotlin.b4 = function (f, self) {
    return function () {
        return f.apply(self, Kotlin.argumentsToArrayLike(arguments));
    }
};
