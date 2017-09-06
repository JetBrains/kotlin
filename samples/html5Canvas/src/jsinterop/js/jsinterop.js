
konan.libraries.push ({
    arenas: [],
    Konan_js_allocateArena: function (array) {
        konan_dependencies.env.arenas.push(array || []);
        return konan_dependencies.env.arenas.length - 1;
        
    },
    Konan_js_freeArena: function(arenaIndex) {
        var arena = konan_dependencies.env.arenas[arenaIndex];
        arena.forEach(function(element, index) {
            arena[index] = null;
        });
        konan_dependencies.env.arenas[arenaIndex] = null;
    },
    Konan_js_addObjectToArena: function (arenaIndex, object) {
        var arena = konan_dependencies.env.arenas[arenaIndex];
        arena.push(object);
        return arena.length - 1;
    },
    Konan_js_wrapLambda: function (index) {
        return (function () { 
            // convert Arguments to an array
            // to be provided by launcher.js
            var arenaIndex = konan_dependencies.env.Konan_js_allocateArena(Array.prototype.slice.call(arguments)); 
            // To be provided by Kotlin runtime. 
            instance.exports.Konan_js_runLambda(index, arenaIndex, arguments.length); 
            konan_dependencies.env.Konan_js_freeArena(arenaIndex);
        });
    },
    Konan_js_getInt: function(arenaIndex, objIndex, propertyNamePtr, propertyNameLength) {
        // TODO:  The toUTF16String() is to be resolved by launcher.js runtime.
        var property = toUTF16String(propertyNamePtr, propertyNameLength); 
        var value =  konan_dependencies.env.arenas[arenaIndex][objIndex][property];
        return value;
    },
    Konan_js_setFunction: function (arena, obj, propertyName, propertyNameLength, func) {
        var name = toUTF16String(propertyName, propertyNameLength);
        konan_dependencies.env.arenas[arena][obj][name] = konan_dependencies.env.Konan_js_wrapLambda(func);
    },
});
