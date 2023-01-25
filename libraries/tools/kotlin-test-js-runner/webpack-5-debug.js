"use strict";

const {SourceMapSource} = require("webpack-sources");
const {absolutify} = require("webpack/lib/util/identifier");

// https://github.com/webpack/webpack/issues/12951
class PatchSourceMapSourcePlugin {
    apply(compiler) {
        compiler.hooks.beforeRun.tap("PatchSourceMapSourcePlugin", compiler => {
            const original = SourceMapSource.prototype._ensureSourceMapObject;

            SourceMapSource.prototype._ensureSourceMapObject = function () {
                original.call(this)
                this._sourceMapAsObject.sources = this._sourceMapAsObject
                    .sources
                    .map(source => {
                        if (!source.startsWith("webpack://")) return source

                        return absolutify(compiler.options.context, source.slice(10))
                    })
            }
        });
    }
}

module.exports = PatchSourceMapSourcePlugin;