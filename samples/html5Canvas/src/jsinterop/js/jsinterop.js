
konan.libraries.push ({
    arenas: new Map(),
    nextArena: 0,
    Konan_js_allocateArena: function (array) {
        var index = konan_dependencies.env.nextArena++;
        konan_dependencies.env.arenas.set(index, array || []);
        return index;
        
    },
    Konan_js_freeArena: function(arenaIndex) {
        var arena = konan_dependencies.env.arenas.get(arenaIndex);
        arena.forEach(function(element, index) {
            arena[index] = null;
        });
        konan_dependencies.env.arenas.delete(arenaIndex);
    },
    Konan_js_pushIntToArena: function (arenaIndex, value) {
        var arena = konan_dependencies.env.arenas.get(arenaIndex);
        arena.push(value);
        return arena.length - 1;
    },
    Konan_js_addObjectToArena: function (arenaIndex, object) {
        var arena = konan_dependencies.env.arenas.get(arenaIndex);
        arena.push(object);
        return arena.length - 1;
    },
    Konan_js_wrapLambda: function (functionArenaIndex, index) {
        return (function () { 
            var functionArena = konan_dependencies.env.arenas.get(functionArenaIndex);

            // convert Arguments to an array
            // to be provided by launcher.js
            var argumentArenaIndex = konan_dependencies.env.Konan_js_allocateArena(Array.prototype.slice.call(arguments));

            var resultIndex = instance.exports.Konan_js_runLambda(index, argumentArenaIndex, arguments.length);
            var result = kotlinObject(argumentArenaIndex, resultIndex);
            konan_dependencies.env.Konan_js_freeArena(argumentArenaIndex);

            return result;
        });
    },
    Konan_js_getInt: function(arenaIndex, objIndex, propertyNamePtr, propertyNameLength) {
        // TODO:  The toUTF16String() is to be resolved by launcher.js runtime.
        var property = toUTF16String(propertyNamePtr, propertyNameLength); 
        var value =  kotlinObject(arenaIndex, objIndex)[property];
        return value;
    },
    Konan_js_getProperty: function(arenaIndex, objIndex, propertyNamePtr, propertyNameLength) {
        // TODO:  The toUTF16String() is to be resolved by launcher.js runtime.
        var property = toUTF16String(propertyNamePtr, propertyNameLength); 
        var arena = konan_dependencies.env.arenas.get(arenaIndex);
        var value = arena[objIndex][property];
        arena.push(value);
        return arena.length - 1;
    },
    Konan_js_setFunction: function (arena, obj, propertyName, propertyNameLength, func) {
        var name = toUTF16String(propertyName, propertyNameLength);
        kotlinObject(arena, obj)[name] = konan_dependencies.env.Konan_js_wrapLambda(arena, func);
    },

    Konan_js_setString: function (arena, obj, propertyName, propertyNameLength, stringPtr, stringLength) {
        var name = toUTF16String(propertyName, propertyNameLength);
        var string = toUTF16String(stringPtr, stringLength);
        kotlinObject(arena, obj)[name] = string;
    },
});

// TODO: This is just a shorthand notation.
function kotlinObject(arenaIndex, objectIndex) {
    var arena = konan_dependencies.env.arenas.get(arenaIndex);
    if (typeof arena == "undefined") {
        console.log("No arena index " + arenaIndex + "for object" + objectIndex);
        console.trace()
    } 
    return arena[objectIndex]
}

function toArena(arenaIndex, object) {
    return konan_dependencies.env.Konan_js_addObjectToArena(arenaIndex, object);
}

