const State = {
    value: "Not OK",
}

define('foo', [], function() {
    return {
        StateProvider: {
            getStateValue: function() { return State.value; }
        }
    }
});

define('bar', [], function() {
    State.value = 'OK';
    return {};
});