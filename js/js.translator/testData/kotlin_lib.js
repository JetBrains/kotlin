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

(function () {
    Kotlin.equals = function (obj1, obj2) {
        if (obj1 == null) {
            return obj2 == null;
        }

        if (Array.isArray(obj1)) {
            return Kotlin.arrayEquals(obj1, obj2);
        }

        if (typeof obj1 == "object" && obj1.equals_za3rmp$ !== undefined) {
            return obj1.equals_za3rmp$(obj2);
        }

        return obj1 === obj2;
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
        return "[" + a.join(", ") + "]";
    };

    Kotlin.intUpto = function (from, to) {
        return new Kotlin.NumberRange(from, to);
    };

    Kotlin.intDownto = function (from, to) {
        return new Kotlin.Progression(from, to, -1);
    };

    Kotlin.modules = {};

    Kotlin.RuntimeException = Kotlin.createClassNow();
    Kotlin.NullPointerException = Kotlin.createClassNow();
    Kotlin.NoSuchElementException = Kotlin.createClassNow();
    Kotlin.IllegalArgumentException = Kotlin.createClassNow();
    Kotlin.IllegalStateException = Kotlin.createClassNow();
    Kotlin.UnsupportedOperationException = Kotlin.createClassNow();
    Kotlin.IOException = Kotlin.createClassNow();

    Kotlin.throwNPE = function () {
        throw new Kotlin.NullPointerException();
    };

    function throwAbstractFunctionInvocationError(funName) {
        return function () {
            var message;
            if (funName !== undefined) {
                message = "Function " + funName + " is abstract";
            }
            else {
                message = "Function is abstract";
            }
            throw new TypeError(message);
        };
    }

    Kotlin.Iterator = Kotlin.createClassNow(null, null, {
        next: throwAbstractFunctionInvocationError("Iterator#next"),
        hasNext: throwAbstractFunctionInvocationError("Iterator#hasNext")
    });

    var ArrayIterator = Kotlin.createClassNow(Kotlin.Iterator,
        function (array) {
            this.array = array;
            this.index = 0;
        }, {
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

    var ListIterator = Kotlin.createClassNow(ArrayIterator,
        function (list) {
            this.list = list;
            this.size = list.size();
            this.index = 0;
        }, {
            next: function () {
                return this.list.get(this.index++);
            }
    });

    Kotlin.Collection = Kotlin.createClassNow();

    Kotlin.Enum = Kotlin.createClassNow(null,
        function () {
            this.name$ = undefined;
            this.ordinal$ = undefined;
        }, {
            name: function () {
                return this.name$;
            },
            ordinal: function () {
                return this.ordinal$;
            },
            toString: function () {
                return this.name();
            }
    });
    (function () {
        function valueOf(name) {
            return this[name];
        }

        function getValues() {
            return this.values$;
        }

        Kotlin.createEnumEntries = function (enumEntryList) {
            var i = 0;
            var values = [];
            for (var entryName in enumEntryList) {
                if (enumEntryList.hasOwnProperty(entryName)) {
                    var entryObject = enumEntryList[entryName];
                    values[i] = entryObject;
                    entryObject.ordinal$ = i;
                    entryObject.name$ = entryName;
                    i++;
                }
            }
            enumEntryList.values$ = values;
            enumEntryList.valueOf_61zpoe$ = valueOf;
            enumEntryList.values = getValues;
            return enumEntryList;
        };
    })();

    Kotlin.PropertyMetadata = Kotlin.createClassNow(null,
        function (name) {
            this.name = name;
        }
    );

    Kotlin.AbstractCollection = Kotlin.createClassNow(Kotlin.Collection, null, {
        addAll_xeylzf$: function (collection) {
            var modified = false;
            var it = collection.iterator();
            while (it.hasNext()) {
                if (this.add_za3rmp$(it.next())) {
                    modified = true;
                }
            }
            return modified
        },
        removeAll_xeylzf$: function (c) {
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
        retainAll_xeylzf$: function (c) {
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
        containsAll_xeylzf$: function (c) {
            var it = c.iterator();
            while (it.hasNext()) {
                if (!this.contains_za3rmp$(it.next())) return false;
            }
            return true;
        },
        isEmpty: function () {
            return this.size() === 0;
        },
        iterator: function () {
            return new ArrayIterator(this.toArray());
        },
        equals_za3rmp$: function (o) {
            if (this.size() !== o.size()) return false;

            var iterator1 = this.iterator();
            var iterator2 = o.iterator();
            var i = this.size();
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
            var i = this.size();
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

    Kotlin.AbstractList = Kotlin.createClassNow(Kotlin.AbstractCollection, null, {
        iterator: function () {
            return new ListIterator(this);
        },
        remove_za3rmp$: function (o) {
            var index = this.indexOf_za3rmp$(o);
            if (index !== -1) {
                this.remove_za3lpa$(index);
                return true;
            }
            return false;
        },
        contains_za3rmp$: function (o) {
            return this.indexOf_za3rmp$(o) !== -1;
        }
    });

    //TODO: should be JS Array-like (https://developer.mozilla.org/en-US/docs/JavaScript/Guide/Predefined_Core_Objects#Working_with_Array-like_objects)
    Kotlin.ArrayList = Kotlin.createClassNow(Kotlin.AbstractList,
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
            size: function () {
                return this.array.length;
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
            addAll_xeylzf$: function (collection) {
                var it = collection.iterator();
                for (var i = this.array.length, n = collection.size(); n-- > 0;) {
                    this.array[i++] = it.next();
                }
            },
            remove_za3lpa$: function (index) {
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
                return "[" + this.array.join(", ") + "]";
            },
            toJSON: function () {
                return this.array;
            },
            checkRange: function (index) {
                if (index < 0 || index >= this.array.length) {
                    throw new RangeError();
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

    Kotlin.RangeIterator = Kotlin.createClassNow(Kotlin.Iterator,
        function (start, end, increment) {
            this.start = start;
            this.end = end;
            this.increment = increment;
            this.i = start;
        }, {
            next: function () {
                var value = this.i;
                this.i = this.i + this.increment;
                return value;
            },
            hasNext: function () {
                return this.i <= this.end;
            }
    });

    Kotlin.NumberRange = Kotlin.createClassNow(null,
        function (start, end) {
            this.start = start;
            this.end = end;
            this.increment = 1;
        }, {
            contains: function (number) {
                return this.start <= number && number <= this.end;
            },
            iterator: function () {
                return new Kotlin.RangeIterator(this.start, this.end);
            }
    });

    Kotlin.Progression = Kotlin.createClassNow(null,
        function (start, end, increment) {
            this.start = start;
            this.end = end;
            this.increment = increment;
        }, {
        iterator: function () {
            return new Kotlin.RangeIterator(this.start, this.end, this.increment);
        }
    });

    Kotlin.Comparator = Kotlin.createClassNow(null, null, {
        compare: throwAbstractFunctionInvocationError("Comparator#compare")
    });

    var ComparatorImpl = Kotlin.createClassNow(Kotlin.Comparator,
        function (comparator) {
            this.compare = comparator;
        }
    );

    Kotlin.comparator = function (f) {
        return new ComparatorImpl(f);
    };

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
            mutableList.set_vux3hl$(i, array[i]);
        }
    };

    Kotlin.copyToArray = function (collection) {
        var array = [];
        var it = collection.iterator();
        while (it.hasNext()) {
            array.push(it.next());
        }

        return array;
    };


    Kotlin.StringBuilder = Kotlin.createClassNow(null,
        function () {
            this.string = "";
        }, {
        append: function (obj) {
            this.string = this.string + obj.toString();
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

    Kotlin.arrayFromFun = function (size, initFun) {
        var result = new Array(size);
        for (var i = 0; i < size; i++) {
            result[i] = initFun(i);
        }
        return result;
    };

    Kotlin.arrayIndices = function (arr) {
        return new Kotlin.NumberRange(0, arr.length - 1);
    };

    Kotlin.arrayIterator = function (array) {
        return new ArrayIterator(array);
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

    Kotlin.jsonAddProperties = function (obj1, obj2) {
        for (var p in obj2) {
            if (obj2.hasOwnProperty(p)) {
                obj1[p] = obj2[p];
            }
        }
        return obj1;
    };
})();

Kotlin.assignOwner = function (f, o) {
    f.o = o;
    return f;
};
