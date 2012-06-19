// Be aware — Google Chrome has serious issue — you can rewrite READ-ONLY property (if it is defined in prototype). Firefox and Safari work correct.
// Always test property access issues in Firefox, but not in Chrome.
var Kotlin = {};

(function () {
    "use strict";

    Kotlin.isType = function (object, klass) {
        if (object === null) {
            return false;
        }

        var proto = Object.getPrototypeOf(object);
        // todo test nested class
        //noinspection RedundantIfStatementJS
        if (proto == klass.proto) {
            return true;
        }

        return false;
    };

    // todo compatibility for opera https://raw.github.com/gist/1000718/es5compat-gs.js
    // proto must be created for class even if it is not needed (requires for is operator)
    Kotlin.createClass = (function () {
        function create(bases, initializer, properties) {
            var proto;
            var baseInitializer = null;
            var isTrait = initializer == null;
            if (!bases) {
                proto = !properties && isTrait ? null : Object.create(null, properties || undefined);
            }
            else if (!Array.isArray(bases)) {
                baseInitializer = bases.initializer;
                proto = !properties && isTrait ? bases.proto : Object.create(bases.proto, properties || undefined);
            }
            else {
                proto = computeProto(bases, properties);
                // first is superclass, other are traits
                baseInitializer = bases[0].initializer;
                // all bases are traits without properties
                if (proto == null && !isTrait) {
                    proto = Object.create(null, properties || undefined);
                }
            }

            var constructor = createConstructor(proto, initializer);
            Object.defineProperty(constructor, "proto", {value: proto});
            Object.defineProperty(constructor, "properties", {value: properties || null});
            // null for trait
            if (!isTrait) {
                Object.defineProperty(constructor, "initializer", {value: initializer});

                Object.defineProperty(initializer, "baseInitializer", {value: baseInitializer});
                Object.seal(initializer);
            }

            Object.seal(constructor);
            return constructor;
        }

        // as separate function to reduce scope
        function createConstructor(proto, initializer) {
            return function () {
                var o = Object.create(proto);
                if (initializer != null) {
                    initializer.apply(o, arguments);
                }
                if (initializer == null || !initializer.hasOwnProperty("skipSeal")) {
                    Object.seal(o);
                }
                return o;
            };
        }

        function computeProto(bases, properties) {
            var proto = null;
            for (var i = 0, n = bases.length; i < n; i++) {
                var base = bases[i];
                var baseProto = base.proto;
                if (baseProto == null || base.properties == null) {
                    continue;
                }

                if (!proto) {
                    proto = Object.create(baseProto, properties || undefined);
                    continue;
                }

                // chrome bug related to getOwnPropertyDescriptor (sometimes returns undefined), so, we keep properties
                //var names = Object.getOwnPropertyNames(baseProto);
                //for (var j = 0, k = names.length; j < k; j++) {
                //    var descriptor = Object.getOwnPropertyDescriptor(baseProto, names[i]);
                //    Object.defineProperty(proto, names[i], descriptor);
                //}
                Object.defineProperties(proto, base.properties);

                // todo test A -> B, C(->D) *properties from D is not yet added to proto*
            }

            return proto;
        }

        return create;
    })();

    Kotlin.createObject = function (initializer, properties) {
        var o = Object.create(null, properties || undefined);
        initializer.call(o);
        return o;
    };

    function emptyFunction() {
    }

    Kotlin.createNamespace = function (initializer, properties, classesAndNestedNamespaces) {
        var o = Object.create(null, properties || undefined);
        Object.defineProperty(o, "initialize", {value: initializer || emptyFunction});
        var keys = Object.keys(classesAndNestedNamespaces);
        for (var i = 0, n = keys.length; i < n; i++) {
            var name = keys[i];
            Object.defineProperty(o, name, {value: classesAndNestedNamespaces[name]});
        }
        return o;
    };

    Kotlin.$new = function (f) {
        return f;
    };

    Kotlin.$createClass = function (parent, properties) {
        if (typeof (parent) != "function") {
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
                    // our std lib contains collision: hasNext property vs hasNext as function, we prefer function (actually, it does work)
                    var getterName = name.substring(4);
                    if (!descriptors.hasOwnProperty(getterName)) {
                        descriptors[getterName] = {get: value};
                        descriptors[name] = {value: value};
                    }
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

        if (initializer) {
            Object.defineProperty(initializer, "skipSeal", {value: true});
        }

        return Kotlin.createClass(parent || null, initializer, descriptors);
    };
})();
