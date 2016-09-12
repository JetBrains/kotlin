//method
void foo() {
	switch (status) {
	    case "init":
	    case "dial":
	    case "transmit":
		return Color.BLACK;

	    case "ok":
		return 0xFF006600;

	    case "cancel":
		return 0xFF666666;

	    case "fail":
	    case "busy":
	    case "error":
	    default:
		return 0xFF660000;
	}
}
