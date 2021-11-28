this["@get something-invalid"] = function() {
    return "something invalid"
}

this["some+value"] = 42

this["+some+object%:"] = {
    foo: "%%++%%"
}
