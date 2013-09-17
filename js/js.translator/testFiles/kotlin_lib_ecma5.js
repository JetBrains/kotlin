
// TODO drop this:
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
})();

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

var KotlinNew = {};

(function () {

    function toArray(obj) {
        var array;
        if (obj == null) {
            array = [];
        }
        else if(!Array.isArray(obj)) {
            array = [obj];
        }
        else {
            array = obj;
        }
        return array;
    }

    function copyProperties(to, from) {
        if (to == null || from == null) {
            return;
        }
        for (var p in from) {
            if (from.hasOwnProperty(p)) {
                to[p] = from[p];
            }
        }
    }

    function getClass(basesArray) {
        for (var i = 0; i < basesArray.length; i++) {
            if (isNativeClass(basesArray[i]) || basesArray[i].$metadata$.type === KotlinNew.TYPE.CLASS) {
                return basesArray[i];
            }
        }
        return null;
    }

    var emptyFunction = function() {};

    KotlinNew.TYPE = {
        CLASS: "class",
        TRAIT: "trait",
        OBJECT: "object"
    };

    KotlinNew.classCount = 0;
    KotlinNew.newClassIndex = function() {
        var tmp = KotlinNew.classCount;
        KotlinNew.classCount++;
        return tmp;
    };

    function isNativeClass(obj) {
        return !(obj == null) && obj.$metadata$ == null;
    }

    function applyExtension(current, bases, baseGetter) {
        for (var i = 0; i < bases.length; i++) {
            if (isNativeClass(bases[i])) {
                continue;
            }
            var base = baseGetter(bases[i]);
            for (var p in  base) {
                if (base.hasOwnProperty(p)) {
                    if(!current.hasOwnProperty(p) || current[p].$classIndex$ < base[p].$classIndex$) {
                        current[p] = base[p];
                    }
                }
            }
        }
    }

    function computeMetadata(bases, properties) {
        var metadata = {};

        metadata.baseClasses = toArray(bases);
        metadata.baseClass = getClass(metadata.baseClasses);
        metadata.classIndex = KotlinNew.newClassIndex();
        metadata.functions = {};
        metadata.properties = {};

        if (!(properties == null)) {
            for (var p in properties) {
                if (properties.hasOwnProperty(p)) {
                    var property = properties[p];
                    property.$classIndex$ = metadata.classIndex;
                    if (typeof property === "function") {
                        metadata.functions[p] = property;
                    } else {
                        metadata.properties[p] = property;
                    }
                }
            }
        }
        applyExtension(metadata.functions, metadata.baseClasses, function (it) {
            return it.$metadata$.functions
        });
        applyExtension(metadata.properties, metadata.baseClasses, function (it) {
            return it.$metadata$.properties
        });

        return metadata;
    }

    function class_object() {
        if (typeof this.$object$ === "undefined") {
            this.$object$ = this.object_initializer$();
        }
        return this.$object$;
    }

    // as separated function to reduce scope // TODO: drop this
    function createConstructor() {
        return function $fun() {
            var initializer = $fun.$metadata$.initializer;
            if (initializer != null) {
                initializer.apply(this, arguments);
            }
        };
    }

    KotlinNew.createClass = function (bases, initializer, properties, staticProperties) {
        var constructor = createConstructor();
        copyProperties(constructor, staticProperties);

        var metadata = computeMetadata(bases, properties);
        metadata.type = KotlinNew.TYPE.CLASS;

        var prototypeObj;
        if (metadata.baseClass !== null) {
            prototypeObj = Object.create(metadata.baseClass.prototype);
        } else {
            prototypeObj = {};
        }
        Object.defineProperties(prototypeObj, metadata.properties);
        copyProperties(prototypeObj, metadata.functions);

        var baseInitializer;
        if (metadata.baseClass !== null && !isNativeClass(metadata.baseClass)) {
            baseInitializer = metadata.baseClass.$metadata$.initializer; // TODO: native superClass
        }
        if (!(initializer == null)) {
            metadata.initializer = initializer;
            metadata.initializer.baseInitializer = baseInitializer;
        } else {
            metadata.initializer = emptyFunction;
        }

        constructor.$metadata$ = metadata;
        constructor.prototype = prototypeObj;
        //Object.defineProperty(constructor, "$object", {get: class_object}); // TODO: check & fix call $object()
        constructor.object$ = class_object;
        return constructor;
    };

    KotlinNew.createObject = function (bases, initializer, functions) {
        var noNameClass = KotlinNew.createClass(bases, initializer, functions);
        var obj = new noNameClass();
        obj.$metadata$ = {
            type: KotlinNew.TYPE.OBJECT
        };
        return  obj;
    };

    KotlinNew.createTrait = function (bases, properties, staticProperties) {
        var obj = function () {};
        copyProperties(obj, staticProperties);

        obj.$metadata$ = computeMetadata(bases, properties);
        obj.$metadata$.type = KotlinNew.TYPE.TRAIT;
        return obj;
    };

    KotlinNew.keys = Object.keys; // TODO drop

    KotlinNew.isType = function (object, klass) {
        if (object == null || klass == null) {
            return false;
        } else {
            return object instanceof klass; // TODO trait support
        }
    };


////////////////////////////////// packages & modules //////////////////////////////

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

    function createDefinition(members) {
        var definition = {};
        if (members == null) {
            return definition;
        }
        for (var p in members) {
            if (members.hasOwnProperty(p)) {
                if ((typeof members[p]) === "function") {
                    definition[p] = members[p];
                } else {
                    Object.defineProperty(definition, p, members[p]);
                }
            }
        }
        return definition;
    }

    KotlinNew.definePackage = function (initializer, members) {
        var definition = createDefinition(members);
        if (initializer === null) {
            return {value: definition};
        }
        else {
            var getter = createPackageGetter(definition, initializer);
            return {get: getter};
        }
    };

    KotlinNew.defineRootPackage = function (initializer, members) {
        var definition = createDefinition(members);

        if (initializer === null) {
            definition.$initializer$ = emptyFunction;
        } else {
            definition.$initializer$ = initializer;
        }
        return definition;
      };

    KotlinNew.defineModule = function (id, module) {
        if (id in Kotlin.modules) {
            throw new Kotlin.IllegalArgumentException();
        }
        module.$initializer$.call(module); // TODO: temporary hack
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

    Kotlin.createTrait = function (bases, properties, staticProperties) {
        return KotlinNew.createTrait(bases, properties, staticProperties);
    };

    Kotlin.createClass = function (bases, initializer, properties, staticProperties) {
        return KotlinNew.createClass(bases, initializer, properties, staticProperties);
    };


    Kotlin.createObject = function (bases, initializer, properties) {
        return KotlinNew.createObject(bases, initializer, properties)
    };

    Kotlin.definePackage = function (initializer, members) {
        return KotlinNew.definePackage(initializer, members);
    };

    Kotlin.defineRootPackage = function (initializer, members) {
        return KotlinNew.defineRootPackage(initializer, members);
    };

    Kotlin.defineModule = function (id, module) {
        KotlinNew.defineModule(id, module);
    };

})();


