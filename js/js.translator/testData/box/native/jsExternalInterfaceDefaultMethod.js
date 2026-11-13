this.Event = function(type) {
    this.type = type;
}

this.EventTarget = function() {}

this.EventTarget.prototype.dispatchEvent = function(event) {
    return event.type;
}
