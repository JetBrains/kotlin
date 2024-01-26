# Translation Order
This document will compile all knowledge about the order of translation of symbols and in which order those symbols shall 
be present in the resulting header.


## Rule 1
We first order symbols by package (alphabetically), then by containing file name, then by class name.

Note: 
Classifiers order will not be further documented here as it is not contextual (at least as we understand it right now)

## Rule 2
If a type is mentioned as supertype, we see that the supertype is declared in the resulting header *before* the using class; 
however, the supertype naming will be ordered *after* the current class 

## Rule 3
If a type is mentioned as return or paramter type, we see a forward declaration being emitted, but the resulting order
of the header is unaffected. 


