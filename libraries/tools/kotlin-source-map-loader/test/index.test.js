var path = require("path");
var fs = require("fs");
var should = require("should");
var loader = require("../");

function execLoader(filename, callback) {
	var async = false;
	var deps = [];
	var warns = [];
	var context = {
		context: path.dirname(filename),
		resolve: function(context, request, callback) {
			process.nextTick(function() {
				var p = path.isAbsolute(request) ? request : path.resolve(context, request);
				if(fs.existsSync(p))
					callback(null, p);
				else
					callback(new Error("File not found"));
			});
		},
		addDependency: function(dep) {
			deps.push(dep);
		},
		emitWarning: function(warn) {
			warns.push(warn);
		},
		callback: function(err, res, map) {
			async = true;
			callback(err, res, map, deps, warns);
		},
		async: function() {
			async = true;
			return this.callback;
		}
	};
	// Remove CRs to make test line ending invariant
	var fixtureContent = fs.readFileSync(filename, "utf-8").replace(/\r/g, '');
	var res = loader.call(context, fixtureContent);
	if(!async) return callback(null, res, null, deps, warns);
}

describe("source-map-loader", function() {
	const fixturesPath = path.join(__dirname, "fixtures");
	const dataPath = path.join(fixturesPath, "data");

	it("should leave normal files untouched", function(done) {
		execLoader(path.join(fixturesPath, "normal-file.js"), function(err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([]);
			should.equal(res, "without SourceMap"),
			should.equal(map, null);
			deps.should.be.eql([]);
			done();
		});
	});

	it("should process inlined SourceMaps", function(done) {
		execLoader(path.join(fixturesPath, "inline-source-map.js"), function(err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([]);
			should.equal(res, "with SourceMap\n// comment"),
			map.should.be.eql({
				"version":3,
				"file":"inline-source-map.js",
				"sources":[
					"inline-source-map.txt"
				],
				"sourcesContent":["with SourceMap"],
				"mappings":"AAAA"
			});
			deps.should.be.eql([]);
			done();
		});
	});

	it("should process external SourceMaps", function(done) {
		execLoader(path.join(fixturesPath, "external-source-map.js"), function(err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([]);
			should.equal(res, "with SourceMap\n// comment"),
			map.should.be.eql({
				"version":3,
				"file":"external-source-map.js",
				"sources":[
					"external-source-map.txt"
				],
				"sourcesContent":["with SourceMap"],
				"mappings":"AAAA"
			});
			deps.should.be.eql([
				path.join(fixturesPath, "external-source-map.map")
			]);
			done();
		});
	});

	it("should process external SourceMaps (external sources)", function(done) {
		execLoader(path.join(fixturesPath, "external-source-map2.js"), function(err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([]);
			should.equal(res, "with SourceMap\n// comment"),
			map.should.be.eql({
				"version":3,
				"file":"external-source-map2.js",
				"sources":[
					path.join(fixturesPath, "external-source-map2.txt")
				],
				"sourcesContent":["with SourceMap"],
				"mappings":"AAAA"
			});
			deps.should.be.eql([
				path.join(dataPath, "external-source-map2.map"),
				path.join(fixturesPath, "external-source-map2.txt")
			]);
			done();
		});
	});

	it("should use last SourceMap directive", function (done) {
		execLoader(path.join(fixturesPath, "multi-source-map.js"), function (err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([]);
			should.equal(res, "with SourceMap\nanInvalidDirective = \"\\n/*# sourceMappingURL=data:application/json;base64,\"+btoa(unescape(encodeURIComponent(JSON.stringify(sourceMap))))+\" */\";\n// comment"),
				map.should.be.eql({
					"version": 3,
					"file": "inline-source-map.js",
					"sources": [
						"inline-source-map.txt"
					],
					"sourcesContent": ["with SourceMap"],
					"mappings": "AAAA"
				});
			deps.should.be.eql([]);
			done();
		});
	});

	it("should skip invalid base64 SourceMap", function (done) {
		execLoader(path.join(fixturesPath, "invalid-inline-source-map.js"), function (err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([]);
			should.equal(res, "without SourceMap\n// @sourceMappingURL=data:application/source-map;base64,\"something invalid\"\n// comment");
			should.equal(map, null);
			deps.should.be.eql([]);
			done();
		});
	});
	it("should warn on invalid base64 SourceMap", function (done) {
		execLoader(path.join(fixturesPath, "invalid-inline-source-map2.js"), function (err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.matchEach(
				new RegExp("Cannot parse inline SourceMap 'invalid\/base64=': SyntaxError: Unexpected token")
			);
			should.equal(res, "without SourceMap\n// @sourceMappingURL=data:application/source-map;base64,invalid/base64=\n// comment");
			should.equal(map, null);
			deps.should.be.eql([]);
			done();
		});
	});

	it("should warn on invalid SourceMap", function (done) {
		execLoader(path.join(fixturesPath, "invalid-source-map.js"), function (err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.matchEach(
				new RegExp("Cannot parse SourceMap 'invalid-source-map.map': SyntaxError: Unexpected string in JSON at position 102")
			);
			should.equal(res, "with SourceMap\n//#sourceMappingURL=invalid-source-map.map\n// comment");
			should.equal(map, null);
			deps.should.be.eql([
				path.join(fixturesPath, "invalid-source-map.map")
			]);
			done();
		});
	});

	it("should warn on missing SourceMap", function(done) {
		execLoader(path.join(fixturesPath, "missing-source-map.js"), function(err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([
				"Cannot find SourceMap 'missing-source-map.map': Error: File not found"
			]);
			should.equal(res, "with SourceMap\n//#sourceMappingURL=missing-source-map.map\n// comment"),
			should.equal(map, null);
			deps.should.be.eql([]);
			done();
		});
	});

	it("should warn on missing source file", function(done) {
		execLoader(path.join(fixturesPath, "missing-source-map2.js"), function(err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([
				"Cannot find source file 'missing-source-map2.txt': Error: File not found"
			]);
			should.equal(res, "with SourceMap\n// comment"),
			map.should.be.eql({
				"version":3,
				"file":"missing-source-map2.js",
				"sources":[
					"missing-source-map2.txt"
				],
				"sourcesContent":[null],
				"mappings":"AAAA"
			});
			deps.should.be.eql([
				path.join(fixturesPath, "missing-source-map2.map")
			]);
			done();
		});
	});

	it("should process inlined SourceMaps with charset", function(done) {
		execLoader(path.join(fixturesPath, "charset-inline-source-map.js"), function(err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([]);
			should.equal(res, "with SourceMap\n// comment"),
			map.should.be.eql({
				"version":3,
				"file":"charset-inline-source-map.js",
				"sources":[
					"charset-inline-source-map.txt"
				],
				"sourcesContent":["with SourceMap"],
				"mappings":"AAAA"
			});
			deps.should.be.eql([]);
			done();
		});
	});

	it("should support absolute sourceRoot paths in sourcemaps", (done) => {
		const sourceRoot = path.join(fixturesPath);
		const javaScriptFilename = "absolute-sourceRoot-source-map.js";
		const sourceFilename = "absolute-sourceRoot-source-map.txt";
		const rootRelativeSourcePath = path.join(sourceRoot, sourceFilename);
		const sourceMapPath = path.join(sourceRoot, "absolute-sourceRoot-source-map.map");

		// Create the sourcemap file
		const rawSourceMap = {
			"version": 3,
			"file": javaScriptFilename,
			"sourceRoot": sourceRoot,
			"sources": [
				sourceFilename
			],
			"mappings": "AAAA"
		};
		fs.writeFileSync(sourceMapPath, JSON.stringify(rawSourceMap));

		execLoader(
			path.join(fixturesPath, javaScriptFilename),
			(err, res, map, deps, warns) => {
				should.equal(err, null);
				warns.should.be.eql([]);
				should.equal(res, "with SourceMap\n// comment"),
				map.should.be.eql({
					"version": 3,
					"file": javaScriptFilename,
					"sources": [
						rootRelativeSourcePath
					],
					"sourcesContent": [
						"with SourceMap\n// comment"
					],
					"mappings": "AAAA"
				});
				deps.should.be.eql([
					sourceMapPath,
					rootRelativeSourcePath
				]);
				done();
			}
		);
	});

	it("should support relative sourceRoot paths in sourcemaps", (done) => {
		const javaScriptFilename = "relative-sourceRoot-source-map.js";
		const sourceFilename = "relative-sourceRoot-source-map.txt";
		const rootRelativeSourcePath = path.join(dataPath, sourceFilename);
		const sourceMapPath = path.join(fixturesPath, "relative-sourceRoot-source-map.map");

		execLoader(
			path.join(fixturesPath, javaScriptFilename),
			(err, res, map, deps, warns) => {
				should.equal(err, null);
				warns.should.be.eql([]);
				should.equal(res, "with SourceMap\n// comment"),
				map.should.be.eql({
					"version": 3,
					"file": javaScriptFilename,
					"sources": [
						rootRelativeSourcePath
					],
					"sourcesContent": [
						"with SourceMap\n// comment"
					],
					"mappings": "AAAA"
				});
				deps.should.be.eql([
					sourceMapPath,
					rootRelativeSourcePath
				]);
				done();
			}
		);
	});
});