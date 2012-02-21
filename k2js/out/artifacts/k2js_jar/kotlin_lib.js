function $A(iterable) {
    if (!iterable) return [];
    if ('toArray' in Object(iterable)) return iterable.toArray();
    var length = iterable.length || 0, results = new Array(length);
    while (length--) results[length] = iterable[length];
    return results;
}

(function () {


    function extend(destination, source) {
        for (var property in source)
            destination[property] = source[property];
        return destination;
    }



    function keys(object) {
        if (Type(object) !== OBJECT_TYPE) {
            throw new TypeError();
        }
        var results = [];
        for (var property in object) {
            if (object.hasOwnProperty(property)) {
                results.push(property);
            }
        }
        return results;
    }

    function values(object) {
        var results = [];
        for (var property in object)
            results.push(object[property]);
        return results;
    }

    extend(Object, {
        extend:extend,
        keys:Object.keys || keys,
        values:values
    });
})();


Object.extend(Function.prototype, (function () {
    var slice = Array.prototype.slice;

    function update(array, args) {
        var arrayLength = array.length, length = args.length;
        while (length--) array[arrayLength + length] = args[length];
        return array;
    }

    function merge(array, args) {
        array = slice.call(array, 0);
        return update(array, args);
    }

    function argumentNames() {
        var names = this.toString().match(/^[\s\(]*function[^(]*\(([^)]*)\)/)[1]
            .replace(/\/\/.*?[\r\n]|\/\*(?:.|[\r\n])*?\*\//g, '')
            .replace(/\s+/g, '').split(',');
        return names.length == 1 && !names[0] ? [] : names;
    }

    function bind(context) {
        if (arguments.length < 2 && Object.isUndefined(arguments[0])) return this;
        var __method = this, args = slice.call(arguments, 1);
        return function () {
            var a = merge(args, arguments);
            return __method.apply(context, a);
        }
    }

    function bindAsEventListener(context) {
        var __method = this, args = slice.call(arguments, 1);
        return function (event) {
            var a = update([event || window.event], args);
            return __method.apply(context, a);
        }
    }

    function wrap(wrapper) {
        var __method = this;
        return function () {
            var a = update([__method.bind(this)], arguments);
            return wrapper.apply(this, a);
        }
    }

    return {
        argumentNames:argumentNames,
        bind:bind,
        bindAsEventListener:bindAsEventListener,
        wrap:wrap
    }
})());

var isType = function (object, klass) {
    current = object.get_class();
    while (current !== klass) {
        if (current === null) {
            return false;
        }
        current = current.superclass;
    }
    return true;
}

var emptyFunction = function () {
}

var Class = (function () {

    function subclass() {
    }

    ;
    function create() {
        var parent = null, properties = $A(arguments);
        if (typeof (properties[0]) == "function")
            parent = properties.shift();

        function klass() {
            this.initializing = klass;
            this.initialize.apply(this, arguments);
        }

        Object.extend(klass, Class.Methods);
        klass.superclass = parent;
        klass.subclasses = [];

        if (parent) {
            subclass.prototype = parent.prototype;
            klass.prototype = new subclass;
            parent.subclasses.push(klass);
        }

        klass.addMethods(
            {
                get_class:function () {
                    return klass;
                }
            });

        if (parent != null) {
            klass.addMethods(
                {
                    super_init:function () {
                        this.initializing = this.initializing.superclass;
                        this.initializing.prototype.initialize.apply(this, arguments)
                    }
                });
        }

        for (var i = 0, length = properties.length; i < length; i++)
            klass.addMethods(properties[i]);

        if (!klass.prototype.initialize)
            klass.prototype.initialize = emptyFunction;

        klass.prototype.constructor = klass;
        return klass;
    }

    function addMethods(source) {
        var ancestor = this.superclass && this.superclass.prototype,
            properties = Object.keys(source);


        for (var i = 0, length = properties.length; i < length; i++) {
            var property = properties[i], value = source[property];
            if (ancestor && (typeof (value) == "function") &&
                value.argumentNames()[0] == "$super") {
                var method = value;
                value = (function (m) {
                    return function () {
                        return ancestor[m].apply(this, arguments);
                    };
                })(property).wrap(method);

            }
            this.prototype[property] = value;
        }

        return this;
    }

    return {
        create:create,
        Methods:{
            addMethods:addMethods
        }
    };
})();

var Trait = (function () {

    function add(object, source) {
        properties = Object.keys(source);
        for (var i = 0, length = properties.length; i < length; i++) {
            var property = properties[i];
            var value = source[property];
            object[property] = value;
        }
        return this;
    }

    function create() {

        result = {}
        for (var i = 0, length = arguments.length; i < length; i++) {
            add(result, arguments[i]);
        }
        return result;
    }

    return {
        create:create
    };
})();


var Namespace = (function () {

    function create() {
        return Trait.create.apply(Trait, arguments);
    }

    return {
        create:create
    };
})();

Kotlin = {}
Kotlin.Class = Class;
Kotlin.Namespace = Namespace;
Kotlin.Trait = Trait;
Kotlin.isType = isType;

Kotlin.equals = function (obj1, obj2) {
    if (typeof obj1 == "object") {
        if (obj1.equals != undefined) {
            return obj1.equals(obj2);
        }
    }
    return (obj1 === obj2);
};

Kotlin.Exceptions = {}
Kotlin.Exceptions.IndexOutOfBounds = {}
Kotlin.array = function (len) {
    return new Kotlin.Array(len, function () {
        return null
    });
}
Kotlin.Array = Class.create({
    initialize:function (len, f) {
        this.array = [];
        var i = 0;
        while (i < len) {
            this.array.push(f(i));
            ++i;
        }
    },
    get:function (index) {
        if ((index < 0) || (index >= this.array.length)) {
            throw Kotlin.Exceptions.IndexOutOfBounds;
        }
        return (this.array)[index];
    },
    set:function (index, value) {
        if ((index < 0) || (index >= this.array.length)) {
            throw Kotlin.Exceptions.IndexOutOfBounds;
        }
        (this.array)[index] = value;
    },
    size:function () {
        return this.array.length;
    },
    iterator:function () {
        return new Kotlin.ArrayIterator(this);
    },
    indices:function() {
        return new Kotlin.NumberRange(0, this.size(), false);
    }
});


Kotlin.ArrayList = Class.create({
    initialize:function () {
        this.array = [];
        this.$size = 0;
    },
    get:function (index) {
        if ((index < 0) || (index >= this.$size)) {
            throw Kotlin.Exceptions.IndexOutOfBounds;
        }
        return (this.array)[index];
    },
    set:function (index, value) {
        if ((index < 0) || (index >= this.$size)) {
            throw Kotlin.Exceptions.IndexOutOfBounds;
        }
        (this.array)[index] = value;
    },
    size:function () {
        return this.$size;
    },
    iterator:function () {
        return new Kotlin.ArrayIterator(this);
    },
    isEmpty:function () {
        return (this.$size == 0);
    },
    add:function (element) {
        this.array[this.$size++] = element;
    },
    addAll:function (collection) {
        var it = collection.iterator();
        while (it.hasNext()) {
            this.add(it.next());
        }
    },
    remove:function (index) {
        for (var i = index; i < this.$size - 1; ++i) {
            this.array[i] = this.array[i + 1];
        }
        this.$size--;
    },
    contains:function (obj) {
        for (var i = 0; i < this.$size; ++i) {
            if (Kotlin.equals(this.array[i], obj)) {
                return true;
            }
        }
        return false;
    }
});


Kotlin.ArrayIterator = Class.create({
    initialize:function (array) {
        this.array = array;
        this.index = 0;
    },
    next:function () {
        return this.array.get(this.index++);
    },
    hasNext:function () {
        return (this.array.size() > this.index);
    }
});

Kotlin.Integer = function () {

    return {
        parseInt:function (str) {
            return parseInt(str);
        }
    }

}();

Kotlin.System = function () {
    var output = "";

    var print = function (obj) {
        if (obj !== undefined) {
            output += obj;
        }
    };
    var println = function (obj) {
        this.print(obj);
        output += "\n";
    };

    return {
        out:function () {
            return {
                print:print,
                println:println
            };
        },
        output:function () {
            return output;
        },
        flush:function () {
            output = "";
        }
    };
}();


Kotlin.ArrayIterator = Class.create({
    initialize:function (array) {
        this.array = array;
        this.index = 0;
    },
    next:function () {
        return this.array.get(this.index++);
    },
    hasNext:function () {
        return (this.array.size() > this.index);
    }
});

Kotlin.RangeIterator = Kotlin.Class.create({initialize:function (start, count, reversed) {
    this.$start = start;
    this.$count = count;
    this.$reversed = reversed;
    this.$i = this.get_start();
}, get_start:function () {
    return this.$start;
}, get_count:function () {
    return this.$count;
}, set_count:function (tmp$0) {
    this.$count = tmp$0;
}, get_reversed:function () {
    return this.$reversed;
}, get_i:function () {
    return this.$i;
}, set_i:function (tmp$0) {
    this.$i = tmp$0;
}, next:function () {
    this.set_count(this.get_count() - 1);
    if (this.get_reversed()) {
        this.set_i(this.get_i() - 1);
        return this.get_i() + 1;
    }
    else {
        this.set_i(this.get_i() + 1);
        return this.get_i() - 1;
    }
}, hasNext:function () {
    return this.get_count() > 0;
}
});

Kotlin.NumberRange = Kotlin.Class.create({initialize:function (start, size, reversed) {
    this.$start = start;
    this.$size = size;
    this.$reversed = reversed;
}, get_start:function () {
    return this.$start;
}, get_size:function () {
    return this.$size;
}, get_reversed:function () {
    return this.$reversed;
}, get_end:function () {
    return this.get_reversed() ? this.get_start() - this.get_size() + 1 : this.get_start() + this.get_size() - 1;
}, contains:function (number) {
    if (this.get_reversed()) {
        return number <= this.get_start() && number > this.get_start() - this.get_size();
    }
    else {
        return number >= this.get_start() && number < this.get_start() + this.get_size();
    }
}, iterator:function () {
    return new Kotlin.RangeIterator(this.get_start(), this.get_size(), this.get_reversed());
}
});
