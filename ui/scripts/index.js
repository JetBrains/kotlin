"use strict";

var ProtoBuf = dcodeIO.ProtoBuf,
	// Protobuf Builders
	waypointsBuilder = ProtoBuf.loadProtoFile("scripts/proto/waypoints.proto"),
	directionBuilder = ProtoBuf.loadProtoFile("scripts/proto/direction.proto"),
	modeChangeBuilder = ProtoBuf.loadProtoFile("scripts/proto/mode_change.proto"),
	genericResponseBuilder = ProtoBuf.loadProtoFile("scripts/proto/generic_response.proto"),
	debugBuilder = ProtoBuf.loadProtoFile("scripts/proto/client_debug.proto"),

	// Protobuf messages
	Waypoints = waypointsBuilder.build("Waypoints"),
	Direction = directionBuilder.build("carkot.DirectionRequest"),
	ModeChange = modeChangeBuilder.build("ModeChange"),
	GenericResponse = genericResponseBuilder.build("GenericResponse"),
	Result = genericResponseBuilder.build("Result"),
	DebugResponse = debugBuilder.build("carkot.DebugResponse"),

	// serverAddress = "http://localhost:8000";
	serverAddress = "http://localhost:7926",
	remoteController = null;

var Commands = {
	UP: 0,
	DOWN: 1,
	LEFT: 2,
	RIGHT: 3,
	properties: {
		0: {name: "UP", value: 0},
		1: {name: "DOWN", value: 1},
		2: {name: "LEFTT", value: 2},
		3: {name: "RIGHT", value: 3}
	}
};

function sendMoveCommand(cmd) {
	if (mode !== "MANUAL") {
		console.log("Wont send the order when not in manual mode!")
		return
	}
	var msg = new Direction(cmd, 0);
	var buffer = msg.toBase64();
	console.log("Sending POST request:" + buffer);
	$.post( serverAddress + "/direction-order", buffer, function(response) {
		drawPath(Waypoints.decode64(response));
	},
	"text");
}

function sendProtobuf(msg, address, callback) {
	var buffer = msg.toBase64();
	$.post( address, buffer, callback);
}

function handleControlKeydown(event) {
	switch (event.which) {
		case 37: // left
			event.preventDefault();
			$( "#left-control-btn" ).addClass("active");
			sendMoveCommand(Commands.LEFT)
		break;
		case 38: // up
			event.preventDefault();
			$( "#up-control-btn" ).addClass("active");
			sendMoveCommand(Commands.UP)
		break;
		case 39: // right
			event.preventDefault();
			$( "#right-control-btn" ).addClass("active");
			sendMoveCommand(Commands.RIGHT)
		break;
		case 40: // down
			event.preventDefault();
			$( "#down-control-btn" ).addClass("active");
			sendMoveCommand(Commands.DOWN)
		break;
	}
}

function handleControlKeyup(event) {
	switch (event.which) {
		case 37: // left
			event.preventDefault();
			$( "#left-control-btn" ).removeClass("active");
		break;
		case 38: // up
			event.preventDefault();
			$( "#up-control-btn" ).removeClass("active");
		break;
		case 39: // right
			event.preventDefault();
			$( "#right-control-btn" ).removeClass("active");
		break;
		case 40: // down
			event.preventDefault();
			$( "#down-control-btn" ).removeClass("active");
		break;
	}
};

// Unfocus buttons after press
$( ".btn" ).mouseup(function(){
    $(this).blur();
});


$(document).keydown(function(event) {
	if (! $( "#controls" ).hasClass("active")) {
		return null
	}
	return handleControlKeydown(event)
});

$(document).keyup(function(event) {
	if (! $( "#controls" ).hasClass("active")) {
		return null
	}
	return handleControlKeyup(event)
});

function changeMode(modeId) {
	console.log("Changing mode to mode #" + modeId)
	var msg = new ModeChange(modeId);
	console.log("Sending request to change algorithm");
	sendProtobuf(msg, serverAddress + "/change-mode", function(response) {
		var response = GenericResponse.decode64(response);
		if (response.result.errorCode != 0) {
			alert("Something went wrong!");
			return;
		}

		console.log("Got OK for changing request")
		// switch(modeId) {
		// 	case 0:
		// 		enterManualMode();
		// 	break;
		// 	case 1:
		// 		buildPerimeter( debug =  false);
		// 	break;
		// 	case 2:
		// 		buildPerimeter(/* debug = */ true);
		// 	break;
		// }
	});
}
