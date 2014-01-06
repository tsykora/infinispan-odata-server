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


---------------
1) Dependencies
---------------

Currently, ISPN OData server is based on modified odata4j libraries, version 0.8.0-SNAPSHOT.
Originally, Samuel Vetsch implemented support for OData actions and functions
(https://bitbucket.org/svetsch/odata4j-actions) and this branch was forked and modified.

Obtain needed libraries from here (Mercurial):
https://bitbucket.org/sykynx/odata4j-actions/

and install:
mvn clean install -DskipTest=true

(skip tests as there are failures in odata4j framework, waiting for fixes from odata4j developers community)

odata4j libraries (version 0.8.0-SNAPSHOT) should be installed in your Maven repository.
(odata4j-core, odata4j-cxf,  odata4j-dist,  odata4j-examples,  odata4j-fit, odata4j-jersey and odata4j-parent)


----------------------
2) Building the server
----------------------

mvn clean package assembly:assembly (-DskipTests=true)

infinispan-odata-server-1.0-SNAPSHOT.jar file should by located in ./target folder.

---------------------
3) Running the server
---------------------

java -jar ./target/infinispan-odata-server-1.0-SNAPSHOT.jar http://localhost:8887/ODataInfinispanEndpoint.svc/ infinispan-dist.xml

The first parameter is URI where the service will be started,
the second parameter is name of a Infinispan configuration file which will be used for starting Infinispan caches.

It can be infinispan-dist.xml or indexing-perf.xml -- already defined as examples in server's resources,
 or an absolute path for custom setting can be passed.

suggested options:
-Xms512m -Xmx512m
-Djava.net.preferIPv4Stack=true
-Dlog4j.configuration=file:///path/to/log4j.xml


---------------------
4) Interface
---------------------

Consumers uses OData as a protocol for communicating with Infinispan OData server (producer).

The service exposes metadata document which describes defined service operations.
Accessible at: http://localhost:8887/ODataInfinispanEndpoint.svc/$metadata

http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_put?[options]
http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_get?[options]
http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_remove?[options]
http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_replace?[options]

See next section for further details.


---------------------
5) Practical usage examples
---------------------

This section introduces basic client-server communication with Infinispan OData server.

CURL tool is used for a few demonstration examples.
Any HTTP client can be used as well.

Example CURL commands are intentionally "one-liners" to be prepared for direct copy and paste into console/terminal.



5a) Putting data into cache

JSON documents are put into caches using POST method with specified "application/json; charset=UTF-8" HTTP content-type header.

It is possible to set up IGNORE_RETURN_VALUES flag -- values are not being returned back to the client after put.



5b) Simple key-value based access




5c) Document store (query based) access








-----------------


Example put into cache looks like:

HTTP POST method
http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_put?key='person1'


"Content-Type", "application/json; charset=UTF-8"

JSON payload:

{"id":"person1",
 "gender":"MALE",
 "firstName":"John",
 "lastName":"Smith",
 "age":24}

curl -X POST -H "Content-Type: application/json" -d '{"id":"person1","name":"Neo"}' http://localhost:8887/ODataInfinispanEndpoint.svc/mySpecialNamedCache_put?key=\'person1\'

curl -X POST -H "Content-Type: application/json" -d '{"id":"person2","name":"Trinity"}' http://localhost:8887/ODataInfinispanEndpoint.svc/mySpecialNamedCache_put?key=\'person2\'




Example get:

HTTP GET method
http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_get?key='person1'

NOTE: it is needed to escape $ sign before 'filter' option and %20 code instead of common spaces

curl -X GET -H "Accept: application/xml;charset=UTF-8" http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_get?\$filter=name%20eq%20\'Neo\'%20or%20name%20eq%20\'trinity\'


There are two approaches, how to obtain stored JSON document.

Direct key-value approach:

http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_get?key='person1'


Document approach, using filter and other query options (cache needs to be configured with indexing enabled)

http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_get?$filter=firstName eq 'John'



this HTTP GET request on this address will filter all JSON documents stored in the myNamedCache
and will return documents which meets specified criteria, i.e. where field "firstName" is equal to "John".




OData specification:

Event -- HTTP Response code | special

Entry not found -- 404
Entry created -- 201 | "location" header with link for getting the entry back | returns back whole stored JSON document
or
Entry created -- 201 (user IGNORE_RETURN_VALUES) | "location" header with link for getting the entry back |
returns back string "Entry created -- ready for access here: " + content of "location" header
