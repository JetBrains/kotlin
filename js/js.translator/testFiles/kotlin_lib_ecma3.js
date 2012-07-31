/*  Prototype JavaScript framework, version 1.6.1
 *  (c) 2005-2009 Sam Stephenson
 *
 *  Prototype is freely distributable under the terms of an MIT-style license.
 *  For details, see the Prototype web site: http://www.prototypejs.org/
 *
 *--------------------------------------------------------------------------*/
var Kotlin = {};

(function () {
    "use strict";
    var emptyFunction = function () {
    };

    Kotlin.argumentsToArrayLike = function (args) {
        var n = args.length;
        var result = new Array(n);
        while (n--) {
            result[n] = args[n];
        }
        return result;
    };

    (function () {
        function extend(destination, source) {
            for (var property in source) {
                destination[property] = source[property];
            }
            return destination;
        }

        function keys(object) {
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
            for (var property in object) {
                results.push(object[property]);
            }
            return results;
        }

        extend(Object, {
            extend: extend,
            keys: Object.keys || keys,
            values: values
        });
    })();

    Object.extend(Function.prototype, (function () {
        function update(array, args) {
            var arrayLength = array.length, length = args.length;
            while (length--) array[arrayLength + length] = args[length];
            return array;
        }

        function argumentNames() {
            var names = this.toString().match(/^[\s\(]*function[^(]*\(([^)]*)\)/)[1]
                .replace(/\/\/.*?[\r\n]|\/\*(?:.|[\r\n])*?\*\//g, '')
                .replace(/\s+/g, '').split(',');
            return names.length == 1 && !names[0] ? [] : names;
        }

        function bindAsEventListener(context) {
            var __method = this, args = Array.prototype.slice.call(arguments, 1);
            return function (event) {
                var a = update([event || window.event], args);
                return __method.apply(context, a);
            };
        }

        function wrap(wrapper) {
            var __method = this;
            return function () {
                var a = update([__method.bind(this)], arguments);
                return wrapper.apply(this, a);
            };
        }

        return {
            argumentNames: argumentNames,
            bindAsEventListener: bindAsEventListener,
            wrap: wrap
        };
    })());

    Kotlin.isType = function (object, klass) {
        if (object === null || object === undefined) {
            return false;
        }

        var current = object.get_class();
        while (current !== klass) {
            if (current === null) {
                return false;
            }
            current = current.superclass;
        }
        return true;
    };

    Kotlin.createTrait = (function () {
        function add(object, source) {
            var properties = Object.keys(source);
            for (var i = 0, length = properties.length; i < length; i++) {
                var property = properties[i];
                object[property] = source[property];
            }
        }

        return function () {
            var result = arguments[0];
            for (var i = 1, n = arguments.length; i < n; i++) {
                add(result, arguments[i]);
            }
            return result;
        }
    })();

    Kotlin.definePackage = function (members) {
        return members === null ? {} : members;
    };

    Kotlin.createClass = (function () {
        var METHODS = {addMethods: addMethods};

        function subclass() {
        }

        function create() {
            var parent = null, properties = Kotlin.argumentsToArrayLike(arguments);
            if (typeof (properties[0]) == "function") {
                parent = properties.shift();
            }

            function klass() {
                this.initializing = klass;
                if (this.initialize) {
                    this.initialize.apply(this, arguments);
                }
            }

            Object.extend(klass, METHODS);
            klass.superclass = parent;
            klass.subclasses = [];

            if (parent) {
                subclass.prototype = parent.prototype;
                klass.prototype = new subclass();
                parent.subclasses.push(klass);
            }

            klass.addMethods(
                {
                    get_class: function () {
                        return klass;
                    }
                });

            if (parent !== null) {
                klass.addMethods(
                    {
                        super_init: function () {
                            this.initializing = this.initializing.superclass;
                            this.initializing.prototype.initialize.apply(this, arguments);
                        }
                    });
            }

            for (var i = 0, length = properties.length; i < length; i++) {
                klass.addMethods(properties[i]);
            }

            if (!klass.prototype.initialize) {
                klass.prototype.initialize = emptyFunction;
            }

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

        return create;
    })();

    Kotlin.$createClass = Kotlin.createClass;

    Kotlin.$new = function (f) {
        var o = {'__proto__': f.prototype};
        return function () {
            f.apply(o, arguments);
            return o;
        };
    };

    Kotlin.createObject = function () {
        var singletonClass = Kotlin.createClass.apply(null, arguments);
        return new singletonClass();
    };

    Kotlin.defineModule = function (id, module) {
        if (id in Kotlin.modules) {
            throw Kotlin.$new(Kotlin.IllegalArgumentException)();
        }

        Kotlin.modules[id] = module;
    };
})();
