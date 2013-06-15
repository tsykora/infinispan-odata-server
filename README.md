OData-ISPN-playground
=====================

OData protocol and Infinispan integration (examples, playground)

This is _personal playground_, don't look for an inspiration here yet.

For now, code is full of dumb, silly and annoying brainstorming comments and many TODOs.
I will clear it and refactor code when I will be done with this.

-----------------

For now: Endpoint is ODataJerseyServer which hosts embedded Infinispan cache manager inside.


Example put into cache looks like:

http://localhost:8887/ODataInfinispanEndpoint.svc/myNamedCache_put?value='simpleValue1'&key='simpleKey1'

Example get:

http://localhost:8887/ODataInfinispanEndpoint.svc/myNamedCache_get?key='simpleKey1'

Pretty easy, isn't it?
