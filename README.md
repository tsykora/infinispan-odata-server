OData-ISPN-playground
=====================

OData protocol and Infinispan integration (examples, playground)

This is _personal playground_, don't look for an inspiration here yet.

For now, code is full of comments and many TODOs.
I will clear it and refactor code once I will be done with this.

-----------------

Build like:

mvn clean package assembly:assembly (-DskipTests=true)

start like:

java -Xms64m -Xmx512m -Djava.net.preferIPv4Stack=true
-Dlog4j.configuration=file:///home/user/log4j_odata.xml -jar
./target/infinispan-odata-server-jar-with-dependencies.jar
http://localhost:8887/ODataInfinispanEndpoint.svc/ infinispan-dist.xml


-----------------

For now: Endpoint is ODataJerseyServer which hosts embedded Infinispan cache manager inside.
It is sufficient to run Runner.java class.
It starts producer thread (starting OData server) and consumer thread for issuing a few basic operations.

Example put into cache looks like:

http://localhost:8887/ODataInfinispanEndpoint.svc/myNamedCache_put?value='simpleValue1'&key='simpleKey1'

Example get:

http://localhost:8887/ODataInfinispanEndpoint.svc/myNamedCache_get?key='simpleKey1'

Pretty easy, isn't it?


There are two approaches, how to get a document.

Straight -- using it's key

http://localhost:8887/ODataInfinispanEndpoint.svc/myNamedCache_get?key='person1'

Document, indexing, searching one, using filter and other query options

http://localhost:8887/ODataInfinispanEndpoint.svc/myNamedCache_get?$filter=firstName eq 'John'

this HTTP GET request on this address will filter all JSON documents stored in the myNamedCache
and will return documents which meets specified criteria, i.e. where field firstName is equal to string John.


Following OData specification:

Event -- HTTP Response code | special

Entry not found -- 404
Entry created -- 201 | "location" header with link for getting the entry back | returns back whole stored JSON document
or
Entry created -- 201 (user IGNORE_RETURN_VALUES) | "location" header with link for getting the entry back |
returns back string "Entry created -- ready for access here: " + content of "location" header
