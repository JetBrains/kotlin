
konan.libraries.push ({          // This one will be auto generated.
        knjs_addDocumentToArena: function(arena) {
            // Should be resolved by jsinterop.
            return toArena(arena, document);
        },
        knjs_setInterval: function (arena, func, interval) {
            setInterval(konan_dependencies.env.Konan_js_wrapLambda(arena, func), interval);
        },
        knjs_getElementById: function (arena, obj, id, idLength, resultArena) {
            var name = toUTF16String(id, idLength);
            var result = kotlinObject(arena, obj).getElementById(name);
            return toArena(resultArena, result);
        },
        knjs_getContext: function (arena, obj, context, contextLength, resultArena) {
            var name = toUTF16String(context, contextLength);
            var result = kotlinObject(arena, obj).getContext(name);
            return toArena(resultArena, result);
        },
        knjs_getBoundingClientRect: function (arena, obj, resultArena) {
            var result = kotlinObject(arena, obj).getBoundingClientRect();
            return toArena(resultArena, result);
        },
        knjs_moveTo: function(arena, obj, x, y) {
            kotlinObject(arena, obj).moveTo(x, y);
        },
        knjs_lineTo: function(arena, obj, x, y) {
            kotlinObject(arena, obj).lineTo(x, y);
        },
        knjs_fillRect: function(arena, obj, x1, y1, width, height) {
            kotlinObject(arena, obj).fillRect(x1, y1, width, height);
        },
        knjs_fillText: function(arena, obj, textPtr, textLength, x, y, maxWidth) {
            kotlinObject(arena, obj).fillText(toUTF16String(textPtr, textLength), x, y, maxWidth);
        },
        knjs_fill: function(arena, obj) {
            kotlinObject(arena, obj).fill();
        },
        knjs_closePath: function(arena, obj) {
            kotlinObject(arena, obj).closePath();
        },
        knjs_setLineWidth: function(arena, obj, value) {
            kotlinObject(arena, obj).lineWidth = value;
        },
        knjs_setFillStyle: function(arena, obj, value_ptr, value_length) {
            var value = toUTF16String(value_ptr, value_length);
            kotlinObject(arena, obj).fillStyle = value;
        },
        knjs_beginPath: function(arena, obj) {
            kotlinObject(arena, obj).beginPath();
        },
        knjs_stroke: function(arena, obj) {
            kotlinObject(arena, obj).stroke();
        },
        knjs_fetch: function(arena, urlPtr, urlLength, resultArena) {
            var url = toUTF16String(urlPtr, urlLength);
            var result = fetch(url);
            return toArena(resultArena, result);
        },
        knjs_then: function(arena, obj, func, resultArena) {
            var value = konan_dependencies.env.Konan_js_wrapLambda(arena, func);
            var result = kotlinObject(arena, obj).then(value);
            var index = toArena(resultArena, result);
            return index;
        },
        knjs_json: function(arena, obj, resultArena) {
            var object = kotlinObject(arena, obj);
            result = object.json();
            return toArena(resultArena, result);
        }
});
