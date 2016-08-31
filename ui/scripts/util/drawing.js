"use strict";

var aspectRatio = 4.0/3.0;

var leftmostPoint = createPoint(0, 0);
var rightmostPoint = createPoint(0, 0);
var topPoint = createPoint(0, 0);
var bottomPoint = createPoint(0, 0);
var eps = 1e-5,
	zero = {
		x: canvas.width / 2,
		y: canvas.height / 2
	},
	carColour = "red",
	referenceColour = "blue",
	foundColour = "blue",
	multiplier = 100;

canvas.style = "border:5px solid #000000;";

var stepX = canvas.width / 20
var stepY = canvas.height / 20

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
	drawPoint(carPosition, carColour, 10);
}

function drawLine(line, colour, lineWidth) {
	var begin_x = 0;
	var begin_y = - (line.A * (0 - zero.x) + line.C) / line.B  + zero.y;

	var end_x = canvas.width;
	var end_y = - (line.A * (canvas.width - zero.x) + line.C) / line.B + zero.y;

	// check if evaluated y-coordinates are in the canvas. If not, re-evaluate x-coordinates from y, to prevent issues with lines that are close to vertical
	if (gt(begin_y, canvas.height) || gt(end_y, canvas.height)) {
		begin_x = - (line.B * (0 - zero.y) + line.C) / line.A + zero.x;
		begin_y = 0;

		end_x = - (line.B * (canvas.height - zero.y) + line.C) / line.A + zero.x;
		end_y = canvas.height;	
	}

	console.log("Drawing line from (" + begin_x + ", " + begin_y + ") to (" + end_x + ", " + end_y + ")");
	ctx.beginPath();
	ctx.moveTo(begin_x, begin_y);
	ctx.lineTo(end_x, end_y);
	if (lineWidth != null) {
		ctx.lineWidth = lineWidth
	}
	ctx.strokeStyle = colour;
	ctx.closePath();
	ctx.stroke();
}

function drawSegment(segment, colour) {
	updateCorners(segment.begin);
	updateCorners(segment.end);
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

function createPoint(x_, y_) {
    var point = {
	x: x_,
	y: y_
    };
    return point;
}

function updateCorners(point) {
	if (point.x < leftmostPoint.x) {
		leftmostPoint = point;
	}
	if (point.x > rightmostPoint.x) {
		rightmostPoint = point;
	}
	if (point.y < bottomPoint.y) {
		bottomPoint = point;
	}
	if (point.y > topPoint.y) {
		topPoint = point;
	}
	resizeCanvas();
}

function drawPoint(point, colour, radius) {
	updateCorners(point);
	ctx.beginPath();
	ctx.arc(point.x + zero.x, point.y + zero.y, radius, 0, 2 * Math.PI, false);
	ctx.fillStyle = colour;
	ctx.closePath();
	ctx.fill();
}

function resizeCanvas() {
		while (canvas.width < rightmostPoint.x + zero.x || 0 > leftmostPoint + zero.x) {
		canvas.width *= 2.0;
		canvas.height = canvas.width / 4.0 * 3.0;
	}

	while (canvas.height < topPoint.y + zero.y || 0 > bottomPoint + zero.y) {
		canvas.height *= 2.0;
		canvas.width = canvas.height / 3.0 * 4.0
	}

	var contentMid = createPoint( (rightmostPoint.x + leftmostPoint.x) / 2, (topPoint.y + bottomPoint.y) / 2);
	var canvasMid = createPoint(canvas.width / 2, canvas.height / 2);

	zero.x = canvasMid.x - contentMid.x
	zero.y = canvasMid.y - contentMid.y

	stepX = canvas.width / 20;
	stepY = canvas.height / 20;
}

function getMousePos(evt) {
	var rect = canvas.getBoundingClientRect();
	var pt = {
	  x: Math.floor((evt.clientX-rect.left) / (rect.right-rect.left) * canvas.width) - zero.x,
	  y: Math.floor((evt.clientY-rect.top) / (rect.bottom-rect.top) * canvas.height - zero.y)
	};
	return pt;
}


function drawAxis() {
	for (var i = zero.x + stepX; i < canvas.width; i += stepX) {
		drawLine(createLine(1, 0, -(i - zero.x)), "black", 0.1);
	}

	for (var i = zero.x - stepX; i > 0; i -= stepX) {
		drawLine(createLine(1, 0, -(i - zero.x)), "black", 0.1);
	}

	drawLine(createLine(1, 0, 0), "black", 2);

	for (var i = zero.y + stepY; i < canvas.height; i += stepY) {
		drawLine(createLine(0, 1, -(i - zero.y)), "black", 0.1);
	}

	for (var i = zero.y - stepY; i > 0; i -= stepY) {
		drawLine(createLine(0, 1, -(i - zero.y)), "black", 0.1);
	}

	drawLine(createLine(0, 1, 0), "black", 2)
}

function drawDebug(data) {
	ctx.clearRect(0, 0, canvas.width, canvas.height);

	var segments = [];
	for (var i = 0; i < data.begin_x.length; i++) {
		var begin = createPoint(data.begin_x[i], data.begin_y[i]);
		var end = createPoint(data.end_x[i], data.end_y[i]);
		var seg = createSegment(begin, end);
		segments.push(seg);
	}

	var points = [];
	for (var i = 0; i < data.x.length; i++) {
		var pt = createPoint(data.x[i], data.y[i])
		points.push(pt);
	}

	var carPosition = createPoint(data.carX, data.carY);

	// draw everything
	console.log("Drawing car position");
	drawCar(carPosition);

	console.log("Drawing segments")
	for (var i = 0; i < segments.length; ++i) {
		drawSegment(segments[i], referenceColour);
	}

	console.log("Drawing points")
	for (var i = 0; i < points.length; i++) {
		drawPoint(points[i], "green", 2);
	}

	drawAxis()
}
