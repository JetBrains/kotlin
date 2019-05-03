/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors. 
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

Kotlin.Kind = {
    CLASS: "class",
    INTERFACE: "interface",
    OBJECT: "object"
};

Kotlin.callGetter = function (thisObject, klass, propertyName) {
    var propertyDescriptor = Object.getOwnPropertyDescriptor(klass, propertyName);
    if (propertyDescriptor != null && propertyDescriptor.get != null) {
        return propertyDescriptor.get.call(thisObject);
    }

    propertyDescriptor = Object.getOwnPropertyDescriptor(thisObject, propertyName);
    if (propertyDescriptor != null && "value" in propertyDescriptor) {
        return thisObject[propertyName];
    }

    return Kotlin.callGetter(thisObject, Object.getPrototypeOf(klass), propertyName);
};

Kotlin.callSetter = function (thisObject, klass, propertyName, value) {
    var propertyDescriptor = Object.getOwnPropertyDescriptor(klass, propertyName);
    if (propertyDescriptor != null && propertyDescriptor.set != null) {
        propertyDescriptor.set.call(thisObject, value);
        return;
    }

    propertyDescriptor = Object.getOwnPropertyDescriptor(thisObject, propertyName);
    if (propertyDescriptor != null && "value" in propertyDescriptor) {
        thisObject[propertyName] = value;
        return
    }

    Kotlin.callSetter(thisObject, Object.getPrototypeOf(klass), propertyName, value);
};

function isInheritanceFromInterface(ctor, iface) {
    if (ctor === iface) return true;

    var metadata = ctor.$metadata$;
    if (metadata != null) {
        var interfaces = metadata.interfaces;
        for (var i = 0; i < interfaces.length; i++) {
            if (isInheritanceFromInterface(interfaces[i], iface)) {
                return true;
            }
        }
    }

    var superPrototype = ctor.prototype != null ? Object.getPrototypeOf(ctor.prototype) : null;
    var superConstructor = superPrototype != null ? superPrototype.constructor : null;
    return superConstructor != null && isInheritanceFromInterface(superConstructor, iface);
}

/**
 *
 * @param {*} object
 * @param {Function|Object} klass
 * @returns {Boolean}
 */
Kotlin.isType = function (object, klass) {
    if (klass === Object) {
        switch (typeof object) {
            case "string":
            case "number":
            case "boolean":
            case "function":
                return true;
            default:
                return object instanceof Object;
        }
    }

    if (object == null || klass == null || (typeof object !== 'object' && typeof object !== 'function')) {
        return false;
    }

    if (typeof klass === "function" && object instanceof klass) {
        return true;
    }

    var proto = Object.getPrototypeOf(klass);
    var constructor = proto != null ? proto.constructor : null;
    if (constructor != null && "$metadata$" in constructor) {
        var metadata = constructor.$metadata$;
        if (metadata.kind === Kotlin.Kind.OBJECT) {
            return object === klass;
        }
    }

    var klassMetadata = klass.$metadata$;

    // In WebKit (JavaScriptCore) for some interfaces from DOM typeof returns "object", nevertheless they can be used in RHS of instanceof
    if (klassMetadata == null) {
        return object instanceof klass;
    }

    if (klassMetadata.kind === Kotlin.Kind.INTERFACE && object.constructor != null) {
        return isInheritanceFromInterface(object.constructor, klass);
    }

    return false;
};

Kotlin.isNumber = function (a) {
    return typeof a == "number" || a instanceof Kotlin.Long;
};

Kotlin.isChar = function (value) {
    return value instanceof Kotlin.BoxedChar
};

Kotlin.isComparable = function (value) {
    var type = typeof value;

    return type === "string" ||
           type === "boolean" ||
           Kotlin.isNumber(value) ||
           Kotlin.isType(value, Kotlin.kotlin.Comparable);
};

Kotlin.isCharSequence = function (value) {
    return typeof value === "string" || Kotlin.isType(value, Kotlin.kotlin.CharSequence);
};