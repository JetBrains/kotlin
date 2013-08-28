

var KotlinNew = {};

(function () {
    'use strict';

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

    if (!Object.keys) {
        Object.keys = function (o) {
            var result = [];
            var i = 0;
            for (var p in o) {
                if (o.hasOwnProperty(p)) {
                    result[i++] = p;
                }
            }
            return result;
        };
    }

    if (!Object.create) {
        Object.create = function(proto) {
            function F() {}
            F.prototype = proto;
            return new F();
        }
    }

    // http://ejohn.org/blog/objectgetprototypeof/
    if ( typeof Object.getPrototypeOf !== "function" ) {
        if ( typeof "test".__proto__ === "object" ) {
            Object.getPrototypeOf = function(object){
                return object.__proto__;
            };
        } else {
            Object.getPrototypeOf = function(object){
                // May break if the constructor has been tampered with
                return object.constructor.prototype;
            };
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    var Util = {};

    Util.toArray = function(obj) {
        var array;
        if (nullOrUndefined(obj)) {
            array = [];
        }
        else if(!Array.isArray(obj)) {
            array = [obj];
        }
        else {
            array = obj;
        }
        return array;
    };

    Util.copyProperties = function(to, from) {
        if (nullOrUndefined(to) || nullOrUndefined(from)) {
            return;
        }
        for (var p in from) {
            if (from.hasOwnProperty(p)) {
                Object.defineProperty(to, p, Object.getOwnPropertyDescriptor(from, p));
                //to[p] = from[p];
            }
        }
    };

    function nullOrUndefined(o) {
        return o === null || o === undefined;
    }

    var emptyFunction = function() {};

    Util.getClass = function (bases) {
        var basesArray = Util.toArray(bases);
        for (var i = 0; i < basesArray.length; i++) {
            if (basesArray[i].$isClass === true) {
                return basesArray[i];
            }
        }
        return null;
    };

    KotlinNew.Util = Util;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    function computePrototype(bases, functions, obj)  {
        if (nullOrUndefined(obj)) {
            obj = {};
        }
        var basesArray = Util.toArray(bases);
        Util.copyProperties(obj, functions);

        for (var i = 0; i < basesArray.length; i++) {
            var basePrototype = basesArray[i].prototype;
            for (var p in basePrototype) {
                if (basePrototype.hasOwnProperty(p)) {
                    if (!obj.hasOwnProperty(p)) {
                        Object.defineProperty(obj, p, Object.getOwnPropertyDescriptor(basePrototype, p));
                    }
                }
            }
        }
        return obj;
    }

    function class_object() {
        if (typeof this.$object$ === "undefined") {
            this.$object$ = this.object_initializer$();
        }
        return this.$object$;
    }

    // as separated function to reduce scope
    function createConstructor() {
        return function $fun() {
            var initializer = $fun.$initializer;
            if (initializer != null) {
                initializer.apply(this, arguments);
            }
        };
    }

    // bases - array, @NotNull, baseClass - _.foo.B, @Nullable
    KotlinNew.createClass = function (bases, baseClass, initializer, functions, staticProperties) {
        var constr = createConstructor();
        var basePrototypeObj;
        var baseInitializer;
        if (!nullOrUndefined(baseClass)) {
            baseInitializer = baseClass.$initializer;
            basePrototypeObj = Object.create(baseClass.prototype);
        }

        Util.copyProperties(constr, staticProperties);
        constr.prototype = computePrototype(bases, functions, basePrototypeObj);
        if (!nullOrUndefined(initializer)) {
            constr.$initializer = initializer;
            constr.$initializer.baseInitializer = baseInitializer;
        } else {
            constr.$initializer = emptyFunction;
        }
        constr.$isClass = true;

        //Object.defineProperty(constr, "$object", {get: class_object}); // TODO: check & fix call $object()
        constr.object$ = class_object;
        return constr;
    };

    KotlinNew.createObject = function (bases, baseClass, initializer, functions) {
        var basePrototypeObj;
        var baseInitializer;
        if (!nullOrUndefined(baseClass)) {
            baseInitializer = baseClass.$initializer;
            basePrototypeObj = Object.create(baseClass.prototype);
        }
        var obj = Object.create(computePrototype(bases, functions, basePrototypeObj));
        if (!nullOrUndefined(initializer)) {
            initializer.baseInitializer = baseInitializer;
            initializer.call(obj);
        }
        obj.$isClass = false;
        return obj;
    };

    KotlinNew.createTrait = function (bases, functions, staticProperties) {
        var obj = function () {};
        obj.prototype = computePrototype(bases, functions);
        Util.copyProperties(obj, staticProperties);
        obj.$isClass = false;
        return obj;
    };

    KotlinNew.keys = Object.keys;

    KotlinNew.isType = function (object, klass) {
        if (nullOrUndefined(object) || nullOrUndefined(klass)) {
            return false;
        } else {
            return object instanceof klass; // TODO trait support
        }
    };


    function createPackageGetter(instance, initializer) {
        return function () {
            if (initializer !== null) {
                var tmp = initializer;
                initializer = null;
                tmp.call(instance);
            }

            return instance;
        };
    }

    KotlinNew.definePackage = function (initializer, members) {
        var definition = Object.create(null, members === null ? undefined : members);
        if (initializer === null) {
            return {value: definition};
        }
        else {
            var getter = createPackageGetter(definition, initializer);
            return {get: getter};
        }
    };

    KotlinNew.defineModule = function (id, module) {
        if (id in Kotlin.modules) {
            throw Kotlin.$new(Kotlin.IllegalArgumentException)();
        }

        Object.defineProperty(Kotlin.modules, id, {value: module});
    };

})();






////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////




// Be aware — Google Chrome has serious issue — you can rewrite READ-ONLY property (if it is defined in prototype). Firefox and Safari work correct.
// Always test property access issues in Firefox, but not in Chrome.
var Kotlin = Object.create(null);

(function () {
    "use strict";

    Kotlin.keys = KotlinNew.keys;

    Kotlin.isType = function (object, type) {
        return KotlinNew.isType(object, type);
    };

    function propertyDescriptorConverter(propertyDescriptor) {
        var obj = {};
        if (propertyDescriptor === null || propertyDescriptor === undefined) {
            return obj;
        }
        for (var p in propertyDescriptor) {
            if (propertyDescriptor.hasOwnProperty(p)) {
                propertyDescriptor[p].enumerable = true;
            }
        }
        Object.defineProperties(obj, propertyDescriptor);
        return obj;
    }


    Kotlin.createTrait = function (bases, properties, staticProperties) {
        return KotlinNew.createTrait(bases, propertyDescriptorConverter(properties), propertyDescriptorConverter(staticProperties));
    };

    Kotlin.createClass = function (bases, initializer, properties, staticProperties) {
        return KotlinNew.createClass(bases, KotlinNew.Util.getClass(bases), initializer, propertyDescriptorConverter(properties), propertyDescriptorConverter(staticProperties));
    };


    Kotlin.createObject = function (bases, initializer, properties) {
        return KotlinNew.createObject(bases, KotlinNew.Util.getClass(bases), initializer, propertyDescriptorConverter(properties))
    };

    Kotlin.definePackage = function (initializer, members) {
        return KotlinNew.definePackage(initializer, members);
    };

    Kotlin.defineModule = function (id, module) {
        KotlinNew.defineModule(id, module);
    };


    Kotlin.$new = function (f) {
        var o = Object.create(f.prototype);
        return function () {
            f.apply(o, arguments);
            return o;
        };
    };

    Kotlin.$createClass = function (parent, properties) {
        if (parent !== null && typeof (parent) != "function") {
            properties = parent;
            parent = null;
        }

        var initializer = null;
        var descriptors = properties ? {} : null;
        if (descriptors != null) {
            var ownPropertyNames = Object.getOwnPropertyNames(properties);
            for (var i = 0, n = ownPropertyNames.length; i < n; i++) {
                var name = ownPropertyNames[i];
                var value = properties[name];
                if (name == "initialize") {
                    initializer = value;
                }
                else if (name.indexOf("get_") === 0) {
                    descriptors[name.substring(4)] = {get: value};
                    // std lib code can refers to
                    descriptors[name] = {value: value};
                }
                else if (name.indexOf("set_") === 0) {
                    descriptors[name.substring(4)] = {set: value};
                    // std lib code can refers to
                    descriptors[name] = {value: value};
                }
                else {
                    // we assume all our std lib functions are open
                    descriptors[name] = {value: value, writable: true};
                }
            }
        }

        return Kotlin.createClass(parent || null, initializer, descriptors);
    };

})();


