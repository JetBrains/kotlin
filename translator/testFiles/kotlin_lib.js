function $A(iterable) {
    if (!iterable) return [];
    if ('toArray' in Object(iterable)) return iterable.toArray();
    var length = iterable.length || 0, results = new Array(length);
    while (length--) results[length] = iterable[length];
    return results;
}

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
        if (Object.isFunction(properties[0]))
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
            if (ancestor && Object.isFunction(value) &&
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

(function () {

    var _toString = Object.prototype.toString,
        NULL_TYPE = 'Null',
        UNDEFINED_TYPE = 'Undefined',
        BOOLEAN_TYPE = 'Boolean',
        NUMBER_TYPE = 'Number',
        STRING_TYPE = 'String',
        OBJECT_TYPE = 'Object',
        FUNCTION_CLASS = '[object Function]',
        BOOLEAN_CLASS = '[object Boolean]',
        NUMBER_CLASS = '[object Number]',
        STRING_CLASS = '[object String]',
        ARRAY_CLASS = '[object Array]',
        DATE_CLASS = '[object Date]';

    function Type(o) {
        switch (o) {
            case null:
                return NULL_TYPE;
            case (void 0):
                return UNDEFINED_TYPE;
        }
        var type = typeof o;
        switch (type) {
            case 'boolean':
                return BOOLEAN_TYPE;
            case 'number':
                return NUMBER_TYPE;
            case 'string':
                return STRING_TYPE;
        }
        return OBJECT_TYPE;
    }

    function extend(destination, source) {
        for (var property in source)
            destination[property] = source[property];
        return destination;
    }

    function inspect(object) {
        try {
            if (isUndefined(object)) return 'undefined';
            if (object === null) return 'null';
            return object.inspect ? object.inspect() : String(object);
        } catch (e) {
            if (e instanceof RangeError) return '...';
            throw e;
        }
    }

    function toJSON(value) {
        return Str('', { '':value }, []);
    }

    function Str(key, holder, stack) {
        var value = holder[key],
            type = typeof value;

        if (Type(value) === OBJECT_TYPE && typeof value.toJSON === 'function') {
            value = value.toJSON(key);
        }

        var _class = _toString.call(value);

        switch (_class) {
            case NUMBER_CLASS:
            case BOOLEAN_CLASS:
            case STRING_CLASS:
                value = value.valueOf();
        }

        switch (value) {
            case null:
                return 'null';
            case true:
                return 'true';
            case false:
                return 'false';
        }

        type = typeof value;
        switch (type) {
            case 'string':
                return value.inspect(true);
            case 'number':
                return isFinite(value) ? String(value) : 'null';
            case 'object':

                for (var i = 0, length = stack.length; i < length; i++) {
                    if (stack[i] === value) {
                        throw new TypeError();
                    }
                }
                stack.push(value);

                var partial = [];
                if (_class === ARRAY_CLASS) {
                    for (var i = 0, length = value.length; i < length; i++) {
                        var str = Str(i, value, stack);
                        partial.push(typeof str === 'undefined' ? 'null' : str);
                    }
                    partial = '[' + partial.join(',') + ']';
                } else {
                    var keys = Object.keys(value);
                    for (var i = 0, length = keys.length; i < length; i++) {
                        var key = keys[i], str = Str(key, value, stack);
                        if (typeof str !== "undefined") {
                            partial.push(key.inspect(true) + ':' + str);
                        }
                    }
                    partial = '{' + partial.join(',') + '}';
                }
                stack.pop();
                return partial;
        }
    }

    function stringify(object) {
        return JSON.stringify(object);
    }

    function toQueryString(object) {
        return $H(object).toQueryString();
    }

    function toHTML(object) {
        return object && object.toHTML ? object.toHTML() : String.interpret(object);
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

    function clone(object) {
        return extend({ }, object);
    }

    function isElement(object) {
        return !!(object && object.nodeType == 1);
    }

    function isArray(object) {
        return _toString.call(object) === ARRAY_CLASS;
    }

    var hasNativeIsArray = (typeof Array.isArray == 'function')
        && Array.isArray([]) && !Array.isArray({});

    if (hasNativeIsArray) {
        isArray = Array.isArray;
    }

    function isHash(object) {
        return object instanceof Hash;
    }

    function isFunction(object) {
        return _toString.call(object) === FUNCTION_CLASS;
    }

    function isString(object) {
        return _toString.call(object) === STRING_CLASS;
    }

    function isNumber(object) {
        return _toString.call(object) === NUMBER_CLASS;
    }

    function isDate(object) {
        return _toString.call(object) === DATE_CLASS;
    }

    function isUndefined(object) {
        return typeof object === "undefined";
    }

    extend(Object, {
        extend:extend,
        inspect:inspect,
        toQueryString:toQueryString,
        toHTML:toHTML,
        keys:Object.keys || keys,
        values:values,
        clone:clone,
        isElement:isElement,
        isArray:isArray,
        isHash:isHash,
        isFunction:isFunction,
        isString:isString,
        isNumber:isNumber,
        isDate:isDate,
        isUndefined:isUndefined
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

    function curry() {
        if (!arguments.length) return this;
        var __method = this, args = slice.call(arguments, 0);
        return function () {
            var a = merge(args, arguments);
            return __method.apply(this, a);
        }
    }

    function delay(timeout) {
        var __method = this, args = slice.call(arguments, 1);
        timeout = timeout * 1000;
        return window.setTimeout(function () {
            return __method.apply(__method, args);
        }, timeout);
    }

    function defer() {
        var args = update([0.01], arguments);
        return this.delay.apply(this, args);
    }

    function wrap(wrapper) {
        var __method = this;
        return function () {
            var a = update([__method.bind(this)], arguments);
            return wrapper.apply(this, a);
        }
    }

    function methodize() {
        if (this._methodized) return this._methodized;
        var __method = this;
        return this._methodized = function () {
            var a = update([this], arguments);
            return __method.apply(null, a);
        };
    }

    return {
        argumentNames:argumentNames,
        bind:bind,
        bindAsEventListener:bindAsEventListener,
        curry:curry,
        delay:delay,
        defer:defer,
        wrap:wrap,
        methodize:methodize
    }
})());

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
