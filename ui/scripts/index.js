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
	DebugResponse = debugBuilder.build("DebugResponse"),

	// serverAddress = "http://localhost:8000";
	serverAddress = "http://localhost:7926",
	remoteController = null,

	// drawing-related objects
	canvas = $( "#pathCanvas" )[0],
	ctx = canvas.getContext('2d'),
	tooltip = $( "#coords-tooltip");

var currentActiveButton = null;

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

$(document).ready(init)

function init() {
	$("#perimeter-mode-tab").hide()
	$("#perimeter-debug-tab").hide()
	$("#manual-mode-tab").hide()
	$("#exit-btn").hide()

	canvas.addEventListener('mousemove', function(evt) {
    	var mousePos = getMousePos(evt);
    	var message = "(" + mousePos.x + "," + mousePos.y + ")";
    	tooltip[0].textContent = message;
    	tooltip.offset( {top: (evt.pageY + 20), left: (evt.pageX + 20) } );
	}, 
	false
);

}

function hideTabs() {
	$("#perimeter-mode-tab").fadeOut()
	$("#perimeter-debug-tab").fadeOut()
	$("#manual-mode-tab").fadeOut()
	$("#exit-btn").fadeOut()
}


function changeMode(modeId, button, associatedTab) {
	console.log("Changing mode to mode #" + modeId)

	// check for pressing wrong buttons
	if (($(button).hasClass("disabled"))) {
		alert("Exit current mode first!");
		return;
	}

	if ($(button) == currentActiveButton) {
		alert("Already in this mode!");
	}

	// if exit pressed
	if (modeId == 0) {
		currentActiveButton.siblings().removeClass("disabled")
		currentActiveButton = null
		hideTabs();
	}
	else {	// entering mode
		$(button).siblings().addClass("disabled");
		currentActiveButton = $(button);
		$("#exit-btn").fadeIn();
		$("#" + associatedTab).fadeIn();
	}

	console.log("Sending request to change mode to" + modeId.toString());
	var msg = new ModeChange(modeId);
	sendProtobuf(msg, serverAddress + "/change-mode", function(response) {
		var response = GenericResponse.decode64(response);
		if (response.result.error != 0) {
			alert("Something went wrong!");
			return;
		}

		console.log("Got OK for changing request")
	});
}
