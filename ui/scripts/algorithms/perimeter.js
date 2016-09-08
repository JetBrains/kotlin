"use strict";

var stopBuildPerimeter = function() {}

function buildPerimeter(debug) {
	console.log("Perimeter building started");
	// perimeterRequest();
	var time = 1000;
	
	if (debug) {
		// var interval = setInterval(perimeterDebugRequest, time);
		return
	} else {
		var interval = setInterval(perimeterRequest, time);
	}

	stopBuildPerimeter = function() {
		clearInterval(interval);
	}
}

function perimeterDebugRequest() {
	console.log("Sending request for debug info");
	var url = "/get-debug";
	$.get(serverAddress + url, {}, function(data) {
		console.log("Got debug response");
		var debugInfo = DebugResponse.decode64(data);
		console.log("Drawing");
		drawDebug(debugInfo, 1);
	});
}

function perimeterRequest(debug) {
	console.log("Sending request for waypoints");
	var url = "/get-waypoints";
	$.get(serverAddress + url, {}, function(data) {
		console.log("Got waypoints response");
		var waypoints = Waypoints.decode64(data);
		if (waypoints.stop) {
			console.log("Stopping");
			stopBuildPerimeter();
		} else {
			console.log("Drawing");
			drawPath(waypoints);
		}
	});
}


function waypointsToString(waypoints) {
	var string = "";
	var x = waypoints.x;
	var y = waypoints.y;
	for (var i = 0; i < x.length; i++) {
		string += x[i].toString() + ", " + y[i].toString() + "\n";
	}
	return string;
};
