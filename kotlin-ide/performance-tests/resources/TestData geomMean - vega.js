/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

{
  "note": "May https://vega.github.io/vega/docs/ be with you",
  "$schema": "https://vega.github.io/schema/vega/v5.json",
  "description": "TestData geomMean",
  "title": "TestData geomMean",
  "width": 800,
  "height": 500,
  "padding": 5,
  "autosize": {"type": "pad", "resize": true},
  "signals": [
    {
      "name": "clear",
      "value": true,
      "on": [
        {"events": "mouseup[!event.item]", "update": "true", "force": true}
      ]
    },
    {
      "name": "shift",
      "value": false,
      "on": [
        {
          "events": "@legendSymbol:click, @legendLabel:click",
          "update": "event.shiftKey",
          "force": true
        }
      ]
    },
    {
      "name": "clicked",
      "value": null,
      "on": [
        {
          "events": "@legendSymbol:click, @legendLabel:click",
          "comment": "note: here `datum` is `selected` data set",
          "update": "{value: datum.value}",
          "force": true
        }
      ]
    },
    {
      "name": "brush",
      "value": 0,
      "on": [
        {"events": {"signal": "clear"}, "update": "clear ? [0, 0] : brush"},
        {"events": "@xaxis:mousedown", "update": "[x(), x()]"},
        {
          "events": "[@xaxis:mousedown, window:mouseup] > window:mousemove!",
          "update": "[brush[0], clamp(x(), 0, width)]"
        },
        {
          "events": {"signal": "delta"},
          "update": "clampRange([anchor[0] + delta, anchor[1] + delta], 0, width)"
        }
      ]
    },
    {
      "name": "anchor",
      "value": null,
      "on": [{"events": "@brush:mousedown", "update": "slice(brush)"}]
    },
    {
      "name": "xdown",
      "value": 0,
      "on": [{"events": "@brush:mousedown", "update": "x()"}]
    },
    {
      "name": "delta",
      "value": 0,
      "on": [
        {
          "events": "[@brush:mousedown, window:mouseup] > window:mousemove!",
          "update": "x() - xdown"
        }
      ]
    },
    {
      "name": "domain",
      "on": [
        {
          "events": {"signal": "brush"},
          "update": "span(brush) ? invert('x', brush) : null"
        }
      ]
    },
    {"name": "timestamp", "value": true, "bind": {"input": "checkbox"}}
  ],
  "data": [
    {
      "name": "table",
      "comment": "To test chart in VEGA editor https://vega.github.io/editor/#/ change `_values` to `values` and rename `url` property",
      "_values" : {
          "hits" : {
            "hits" : [
              {
                "_source" : {"build_id" : 87834896, "build.timestamp" : "2020-09-21T21:00:31+0000", "geomMean" : 37, "benchmark" : "highlight"}
              },
              {
                "_source" : {"build_id" : 87783396, "build.timestamp" : "2020-09-21T12:34:19+0000", "geomMean" : 37, "benchmark" : "highlight"}
              },
              {
                "_source" : {"build_id" : 87809918, "build.timestamp" : "2020-09-21T16:47:11+0000", "geomMean" : 37, "benchmark" : "highlight"}
              },
              {
                "_source" : {"build_id" : 87905203, "build.timestamp" : "2020-09-22T13:23:44+0000", "geomMean" : 37, "benchmark" : "highlight"}
              },
              {
                "_source" : {"build_id" : 87894638, "build.timestamp" : "2020-09-22T09:12:16+0000", "geomMean" : 37, "benchmark" : "highlight"}
              }
            ]
          }
      },
      "url": {
        "comment": "source index pattern",
        "index": "kotlin_ide_benchmarks*",
        "comment": "it's a body of ES _search query to check query place it into `POST /kotlin_ide_benchmarks*/_search`",
        "comment": "it uses Kibana specific %timefilter% for time frame selection",
        "body": {
          "size": 1000,
          "query": {
            "bool": {
              "must": [
                {"term": {"benchmark.keyword": "highlight"}},
                {"range": {"build.timestamp": {"%timefilter%": true}}}
              ]
            }
          },
          "_source": ["build_id", "benchmark", "build.timestamp", "geomMean"],
          "sort": [{"build.timestamp": {"order": "asc"}}]
        }
      },
      "format": {"property": "hits.hits"},
      "comment": "we need to have follow data: \"build_id\", \"metric_name\", \"metric_value\" and \"metric_error\"",
      "comment": "so it has to be array of {\"build_id\": \"...\", \"metric_name\": \"...\", \"metric_value\": ..., \"metric_error\": ...}",
      "transform": [
        {"type": "collect","sort": {"field": "_source.build\\.timestamp"}},
        {
          "comment": "make alias: _source.build_id -> build_id",
          "type": "formula",
          "as": "build_id",
          "expr": "datum._source.build_id"
        },
        {
          "comment": "make alias: _source.benchmark -> metric_name",
          "type": "formula",
          "as": "metric_name",
          "expr": "datum._source.benchmark"
        },
        {
          "comment": "make alias: _source.geomMean -> metric_value",
          "type": "formula",
          "as": "metric_value",
          "expr": "datum._source.geomMean"
        },
        {
          "comment": "define metric_error",
          "type": "formula",
          "as": "metric_error",
          "expr": "0"
        },
        {
          "type": "formula",
          "as": "timestamp",
          "expr": "timeFormat(toDate(datum._source['build.timestamp']), '%Y-%m-%d %H:%M')"
        },
        {
          "comment": "create `url` value that points to TC build",
          "type": "formula",
          "as": "url",
          "expr": "'https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_Benchmarks_PluginPerformanceTests_IdeaPluginPerformanceTests/' + datum._source.build_id"
        }
      ]
    },
    {
      "name": "selected",
      "on": [
        {"trigger": "clear", "remove": true},
        {"trigger": "!shift", "remove": true},
        {"trigger": "!shift && clicked", "insert": "clicked"},
        {"trigger": "shift && clicked", "toggle": "clicked"}
      ]
    }
  ],
  "axes": [
    {
      "scale": "x",
      "grid": true,
      "domain": false,
      "orient": "bottom",
      "labelAngle": -20,
      "labelAlign": "right",
      "title": {"signal": "timestamp ? 'timestamp' : 'build_id'"},
      "titlePadding": 10,
      "tickCount": 5,
      "encode": {
        "labels": {
          "interactive": true,
          "update": {"tooltip": {"signal": "datum.label"}}
        }
      }
    },
    {
      "scale": "y",
      "grid": true,
      "domain": false,
      "orient": "left",
      "titlePadding": 10,
      "title": "ms",
      "titleAnchor": "end",
      "titleAngle": 0
    }
  ],
  "scales": [
    {
      "name": "x",
      "type": "point",
      "range": "width",
      "domain": {"data": "table", "field": {"signal": "timestamp ? 'timestamp' : 'build_id'"}}
    },
    {
      "name": "y",
      "type": "linear",
      "range": "height",
      "nice": true,
      "zero": true,
      "domain": {"data": "table", "field": "metric_value"}
    },
    {
      "name": "color",
      "type": "ordinal",
      "range": "category",
      "domain": {"data": "table", "field": "metric_name"}
    },
    {
      "name": "size",
      "type": "linear",
      "round": true,
      "nice": false,
      "zero": true,
      "domain": {"data": "table", "field": "metric_error"},
      "range": [1, 100]
    }
  ],
  "legends": [
    {
      "title": "Cases",
      "stroke": "color",
      "strokeColor": "#ccc",
      "padding": 8,
      "cornerRadius": 4,
      "symbolLimit": 50,
      "encode": {
        "symbols": {
          "name": "legendSymbol",
          "interactive": true,
          "update": {
            "fill": {"value": "transparent"},
            "strokeWidth": {"value": 2},
            "opacity": [
              {
                "comment": "here `datum` is `selected` data set",
                "test": "!length(data('selected')) || indata('selected', 'value', datum.value)",
                "value": 0.7
              },
              {"value": 0.15}
            ],
            "size": {"value": 64}
          }
        },
        "labels": {
          "name": "legendLabel",
          "interactive": true,
          "update": {
            "opacity": [
              {
                "comment": "here `datum` is `selected` data set",
                "test": "!length(data('selected')) || indata('selected', 'value', datum.value)",
                "value": 1
              },
              {"value": 0.25}
            ]
          }
        }
      }
    }
  ],
  "marks": [
    {
      "type": "group",
      "from": {
        "facet": {"name": "series", "data": "table", "groupby": "metric_name"}
      },
      "marks": [
        {
          "type": "line",
          "from": {"data": "series"},
          "encode": {
            "hover": {"opacity": {"value": 1}, "strokeWidth": {"value": 4}},
            "update": {
              "x": {"scale": "x", "field": {"signal": "timestamp ? 'timestamp' : 'build_id'"}},
              "y": {"scale": "y", "field": "metric_value"},
              "strokeWidth": {"value": 2},
              "opacity": [
                {
                  "test": "(!domain || inrange(datum.build_id, domain)) && (!length(data('selected')) || indata('selected', 'value', datum.metric_name))",
                  "value": 0.7
                },
                {"value": 0.15}
              ],
              "stroke": [
                {
                  "test": "(!domain || inrange(datum.build_id, domain)) && (!length(data('selected')) || indata('selected', 'value', datum.metric_name))",
                  "scale": "color",
                  "field": "metric_name"
                },
                {"value": "#ccc"}
              ]
            }
          }
        },
        {
          "type": "symbol",
          "from": {"data": "series"},
          "encode": {
            "enter": {
              "tooltip": {
                "signal": "datum.metric_name + ': ' + datum.metric_value + ' ms'"
              },
              "href": {"field": "url"},
              "cursor": {"value": "pointer"},
              "size": {"scale": "size", "field": "metric_error"},
              "x": {"scale": "x", "field": {"signal": "timestamp ? 'timestamp' : 'build_id'"}},
              "y": {"scale": "y", "field": "metric_value"},
              "strokeWidth": {"value": 1},
              "fill": {"scale": "color", "field": "metric_name"}
            },
            "update": {
              "opacity": [
                {
                  "test": "(!domain || inrange(datum.build_id, domain)) && (!length(data('selected')) || indata('selected', 'value', datum.metric_name))",
                  "value": 1
                },
                {"value": 0.15}
              ]
            }
          },
          "zindex": 2
        }
      ]
    }
  ]
}
