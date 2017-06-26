var full = {
    ' SimpleTest testFoo': 'fail',
    ' SimpleTest testBar': 'pass',
    ' SimpleTest testFooWrong': 'pending',
    ' TestTest emptyTest': 'pending',
    ' org OrgTest test': 'pass',
    ' org some SomeTest test': 'pass',
    ' org some name NameTest test': 'pass',
    ' org other name NameTest test': 'pass'
};

// Filter out pending tests for Tape
var noPending = {};
for (var name in full) {
    var result = full[name];
    if (result !== 'pending') {
        noPending[name] = result;
    }
}

module.exports.full = full;
module.exports.noPending = noPending;