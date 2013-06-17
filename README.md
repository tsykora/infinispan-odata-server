OData-ISPN-playground
=====================

OData protocol and Infinispan integration (examples, playground)

This is _personal playground_, don't look for an inspiration here yet.

For now, code is full of comments and many TODOs.
I will clear it and refactor code once I will be done with this.

-----------------

For now: Endpoint is ODataJerseyServer which hosts embedded Infinispan cache manager inside.
It is sufficient to run Runner.java class.
It starts producer thread (starting OData server) and consumer thread for issuing a few basic operations.

Example put into cache looks like:

http://localhost:8887/ODataInfinispanEndpoint.svc/myNamedCache_put?value='simpleValue1'&key='simpleKey1'

Example get:

http://localhost:8887/ODataInfinispanEndpoint.svc/myNamedCache_get?key='simpleKey1'

Pretty easy, isn't it?
