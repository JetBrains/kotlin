
konan.libraries.push ({          // This one will be auto generated.
        knjs_addDocumentToArena: function(arena) {
            // Should be resolved by jsinterop.
            return konan_dependencies.env.Konan_js_addObjectToArena(arena, document);
        },
        knjs_setInterval: function (func, interval) {
            setInterval(konan_dependencies.env.Konan_js_wrapLambda(func), interval);
        },
        knjs_getElementById: function (arena, obj, id, idLength) {
            var name = toUTF16String(id, idLength);
            var result = konan_dependencies.env.arenas[arena][obj].getElementById(name);
            return konan_dependencies.env.Konan_js_addObjectToArena(arena, result);
        },
        knjs_getContext: function (arena, obj, context, contextLength) {
            var name = toUTF16String(context, contextLength);
            var result = konan_dependencies.env.arenas[arena][obj].getContext(name);
            return konan_dependencies.env.Konan_js_addObjectToArena(arena, result);
        },
        knjs_getBoundingClientRect: function (arena, obj) {
            var result = konan_dependencies.env.arenas[arena][obj].getBoundingClientRect();
            return konan_dependencies.env.Konan_js_addObjectToArena(arena, result);
        },
        knjs_moveTo: function(arena, obj, x, y) {
            konan_dependencies.env.arenas[arena][obj].moveTo(x, y);
        },
        knjs_lineTo: function(arena, obj, x, y) {
            konan_dependencies.env.arenas[arena][obj].lineTo(x, y);
        },
        knjs_stroke: function(arena, obj) {
            konan_dependencies.env.arenas[arena][obj].stroke();
        },
});
