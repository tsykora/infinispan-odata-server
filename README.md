OData-ISPN-playground
=====================

Infinispan OData server

is a server based on odata4j framework (its core is running on OData Jersey server) and using Infinispan
as underlying JSON document cache store.

Clients are allowed to use OData standard query language and OData service operation
for communication with the server, storing and obtaining requested JSON documents.

Infinispan OData server understands an internal structure of JSON document,
and therefore, it is possible to query the cache and ask for a~collection
of results in dependence on specified expression operators.

Infinispan OData server also supports common, key-value access approach
for accessing JSON documents.


Outline of this README file:

1) Dependencies
2) Building the server
3) Running the server

4) Communication with server (basics)

5) Practical usage examples
5a) Simple key-value based access
5b) Document store (query based) access

-----------------

1) Dependencies

Currently, ISPN OData server is based on odata4j-actions project, version 0.8.0-SNAPSHOT,
which was modified for implementation needs.

Obtain needed libraries from here (Mercurial):
https://bitbucket.org/sykynx/odata4j-actions/

and install:
mvn clean install -DskipTest=true

(skip tests as there are failures in odata4j framework, waiting for fixes from odata4j developers community)

odata4j libraries (version 0.8.0-SNAPSHOT) should be installed in your Maven repository.
(odata4j-core, odata4j-cxf,  odata4j-dist,  odata4j-examples,  odata4j-fit, odata4j-jersey and odata4j-parent)


2) Building the server

mvn clean package assembly:assembly (-DskipTests=true)




3) Running the server

4) Communication with server (basics)

5) Practical usage examples
5a) Simple key-value based access
5b) Document store (query based) access










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
