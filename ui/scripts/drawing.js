"use strict";

var canvas = $( "#pathCanvas" )[0],
	ctx = canvas.getContext('2d'),
	eps = 1e-5,
	zero = {
		x: canvas.width / 2,
		y: canvas.height / 4 * 3
	},
	carColour = "red",
	referenceColour = "green",
	foundColour = "blue",
	multiplier = 100;

canvas.style = "border:5px solid #000000;";

function drawPath(waypoints) {
	ctx.beginPath();
	ctx.moveTo(canvas.width / 2, canvas.height / 2);
	for (var i = 0; i < waypoints.begin_x.length; i++) {
		ctx.moveTo(waypoints.begin_x[i] + canvas.width / 2, waypoints.begin_y[i] + canvas.height / 2);
		ctx.lineTo(waypoints.end_x[i] + canvas.width / 2, waypoints.end_y[i] + canvas.height / 2);

	};
	ctx.closePath();
	ctx.stroke();
}

function drawCar(carPosition) {
	ctx.beginPath();
	ctx.arc(carPosition.x + zero.x, carPosition.y + zero.y, 10, 0, 2 * Math.PI, false);
	ctx.fillStyle = carColour;
	ctx.closePath();	
	ctx.fill();
}

function drawLine(line) {
	var begin_x = 0;
	var begin_y = (line.A * (0 - zero.x) + line.C) / line.B  + zero.y;

	var end_x = canvas.width;
	var end_y = (line.A * (canvas.width - zero.x) + line.C) / line.B + zero.y;

	// check if evaluated y-coordinates are in the canvas. If not, re-evaluate x-coordinates from y, to prevent issues with lines that are close to vertical
	if (gt(begin_y, canvas.height) || gt(end_y, canvas.height)) {
		begin_x = (line.B * (0 - zero.y) + line.C) / line.A + zero.x;
		begin_y = 0;

		end_x = (line.B * (canvas.height - zero.y) + line.C) / line.A + zero.x;
		end_y = canvas.height;	
	}

	console.log("Drawing line from (" + begin_x + ", " + begin_y + ") to (" + end_x + ", " + end_y + ")");
	ctx.beginPath();
	ctx.moveTo(begin_x, begin_y);
	ctx.lineTo(end_x, end_y);
	ctx.strokeStyle = foundColour;
	ctx.closePath();
	ctx.stroke();
}

function drawSegment(segment, colour) {
	console.log("Drawing segment from (" + segment.begin.x + ", " + segment.begin.y + ") to (" + segment.end.x + ", " + segment.end.y + ")")
	ctx.beginPath();
	ctx.moveTo(segment.begin.x + zero.x, segment.begin.y + zero.y);
	ctx.lineTo(segment.end.x + zero.x, segment.end.y + zero.y);
	ctx.strokeStyle = colour;
	ctx.closePath();	
	ctx.stroke();
}

function eq(lhs, rhs) {
	return Math.abs(lhs - rhs) < eps;
}

function lt(lhs, rhs) {
	return lhs - rhs < -eps;
}

function gt(lhs, rhs) {
	return lhs - rhs > eps;
}

function intersectLines(l1, l2) {
	// Kramer's dets
	var delta1 = l1.A * l2.B - l1.B * l2.A;
	var delta2 = l1.C * l2.B - l1.B * l2.C;
	var delta3 = l1.A * l2.C - l1.C * l2.A;

	if (eq(delta1, 0)) {
		alert("Lines are parallel, can't intersect!");
		return null
	}

	var x = delta2 / delta1;
	var y = delta3 / delta1;
	var point = {
		x: x,
		y: y
	};

	return point;
}

function createSegment(begin_, end_) {
	var segment = {
		begin: begin_,
		end: end_
	}
	return segment
}

function createLine(A, B, C) {
	var line = {
		A: A,
		B: B,
		C: C
	}
	return line
}

function drawDebug(data) {
	ctx.clearRect(0, 0, canvas.width, canvas.height);

	// Parse protobuf into geometry primitives
	var foundLines = [];
	for (var i = 0; i < data.A.length; i++) {
		foundLines.push(createLine(data.A[i] / multiplier, data.B[i] / multiplier, data.C[i] / multiplier));
	}

	var refLines = [];
	for (var i = 0; i < data.Aref.length; i++) {
		refLines.push(createLine(data.Aref[i] / multiplier, data.Bref[i] / multiplier, data.Cref[i] / multiplier));
	}

	var carPosition = {
		x: data.carX,
		y: data.carY, 
		angle: data.carAngle 
	};

	// Build sequence of segments from lines 
	var lastPoint = {
		x: 0,
		y: 0
	};

	

	var referenceSegments = [];
	var corners = [];
	for (var i = 0; i < refLines.length - 1; ++i) {
		var curLine = refLines[i];
		var nextLine = refLines[i + 1];

		var intersectionPoint = intersectLines(curLine, nextLine);
		corners.push(intersectionPoint);
	}

	corners.push(intersectLines(refLines[refLines.length - 1], refLines[0]));
	for (var i = 0; i < corners.length - 1; ++i) {
		var curCorner = corners[i];
		var nextCorner = corners[i + 1];

		referenceSegments.push(createSegment(curCorner, nextCorner));
	}
	referenceSegments.push(createSegment(corners[corners.length - 1], corners[0]));

	// draw everything
	console.log("Drawing car position");
	drawCar(carPosition);

	console.log("Drawing found Lines");
	for (var i = 0; i < foundLines.length; ++i) {
		drawLine(foundLines[i]);
	}
	console.log("Drawing referenceSegments")
	for (var i = 0; i < referenceSegments.length; ++i) {
		drawSegment(referenceSegments[i], referenceColour);
	}
}