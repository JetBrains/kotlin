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
	multiplier = 1e6;

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

	var foundSegments = [];
	for (var i = 0; i < foundLines.length - 1; ++i) {
		var curLine = foundLines[i];
		var nextLine = foundLines[i + 1];

		var intersectionPoint = intersectLines(curLine, nextLine);
		foundSegments.push(createSegment(lastPoint, intersectionPoint));
		lastPoint = intersectionPoint;
	}

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
	drawCar(carPosition);
		
	console.log("Drawing foundSegments")
	for (var i = 0; i < foundSegments.length; ++i) {
		drawSegment(foundSegments[i], foundColour);
	}

	console.log("Drawing referenceSegments")
	for (var i = 0; i < referenceSegments.length; ++i) {
		drawSegment(referenceSegments[i], referenceColour);
	}
}