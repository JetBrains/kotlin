(function() {
    var status = exampleapp.exampleapp.status;
    if (status !== "foo") {
        throw new Error("Unexpected status: " + status);
    }
})();
