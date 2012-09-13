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

    if (!Array.isArray) {
        Array.isArray = function (vArg) {
            return Object.prototype.toString.call(vArg) === "[object Array]";
        };
    }
    if (!Function.prototype.bind) {
        Function.prototype.bind = function (oThis) {
            if (typeof this !== "function") {
                // closest thing possible to the ECMAScript 5 internal IsCallable function
                throw new TypeError("Function.prototype.bind - what is trying to be bound is not callable");
            }

            var aArgs = Array.prototype.slice.call(arguments, 1),
                fToBind = this,
                fNOP = function () {
                },
                fBound = function () {
                    return fToBind.apply(this instanceof fNOP && oThis
                                             ? this
                                             : oThis,
                                         aArgs.concat(Array.prototype.slice.call(arguments)));
                };

            fNOP.prototype = this.prototype;
            fBound.prototype = new fNOP();

            return fBound;
        };
    }

    Kotlin.keys = Object.keys || function (o) {
        var result = [];
        var i = 0;
        for (var p in o) {
            if (o.hasOwnProperty(p)) {
                result[i++] = p;
            }
        }
        return result;
    };

    function copyProperties(to, from) {
        for (var p in from) {
            if (from.hasOwnProperty(p)) {
                to[p] = from[p];
            }
        }
    }

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

    Kotlin.createTrait = function () {
        var result = arguments[0];
        for (var i = 1, n = arguments.length; i < n; i++) {
            copyProperties(result, arguments[i]);
        }
        return result;
    };

    Kotlin.definePackage = function (members) {
        return members === null ? {} : members;
    };

    Kotlin.createClass = (function () {
        function subclass() {
        }

        function create(parent, properties, staticProperties) {
            var traits = null;
            if (parent instanceof Array) {
                traits = parent;
                parent = parent[0];
            }

            function klass() {
                this.initializing = klass;
                if (this.initialize) {
                    this.initialize.apply(this, arguments);
                }
            }

            klass.addMethods = addMethods;
            klass.superclass = parent || null;
            klass.subclasses = [];

            if (parent) {
                if (typeof (parent) == "function") {
                    subclass.prototype = parent.prototype;
                    klass.prototype = new subclass();
                    parent.subclasses.push(klass);
                }
                else {
                    // trait
                    klass.addMethods(parent);
                }
            }

            klass.addMethods({get_class: function () {
                return klass;
            }});

            if (parent !== null) {
                klass.addMethods({super_init: function () {
                    this.initializing = this.initializing.superclass;
                    this.initializing.prototype.initialize.apply(this, arguments);
                }});
            }

            if (traits !== null) {
                for (var i = 1, n = traits.length; i < n; i++) {
                    klass.addMethods(traits[i]);
                }
            }
            if (properties !== null && properties !== undefined) {
                klass.addMethods(properties);
            }

            if (!klass.prototype.initialize) {
                klass.prototype.initialize = emptyFunction;
            }

            klass.prototype.constructor = klass;
            if (staticProperties !== null && staticProperties !== undefined) {
                copyProperties(klass, staticProperties);
            }
            return klass;
        }

        function addMethods(source) {
            copyProperties(this.prototype, source);
            return this;
        }

        return create;
    })();

    Kotlin.$createClass = function (parent, properties) {
        if (parent !== null && typeof (parent) != "function") {
            properties = parent;
            parent = null;
        }
        return Kotlin.createClass(parent, properties, null);
    };

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
