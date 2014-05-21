Infinispan OData server
======================

is a standalone server based on odata4j framework (its core is running on OData Jersey server) and using Infinispan
as underlying JSON document cache store.

Clients are allowed to use OData standard query language and OData service operation
for communication with the server, storing and obtaining requested JSON documents.

Infinispan OData server understands an internal structure of JSON document,
and therefore, it is possible to query the cache and ask for a collection
of results in dependence on specified expression operators.

Infinispan OData server also supports common, key-value access approach
for accessing JSON documents.

Current plans: This is prototype version in odata4j branch -- based on odata4j framework. This will be probably
deferred later on as we are working on a migration to Apache Olingo.


Outline of this README file:

1) Dependencies

2) Building the server

3) Running the server

4) Communication with server

5) Practical usage examples

5a) Simple key-value based access

5b) Document store (query based) access


---------------
1) Dependencies
---------------

Currently, ISPN OData server is based on modified odata4j libraries, version 0.8.0-SNAPSHOT.

Originally, Samuel Vetsch implemented support for OData actions, functions and service operations

(https://bitbucket.org/svetsch/odata4j-actions) and this branch was forked and modified.

Obtain needed libraries from here (Mercurial):

https://bitbucket.org/sykynx/odata4j-actions/

And install (Apache Maven is needed):

mvn clean install -DskipTest=true

(skip tests as there is a problem with missing dependencies in odata4j framework)

odata4j libraries (version 0.8.0-SNAPSHOT) should be installed in your Maven repository.

(odata4j-core, odata4j-cxf,  odata4j-dist,  odata4j-examples,  odata4j-fit, odata4j-jersey and odata4j-parent)


----------------------
2) Building the server
----------------------

mvn clean package assembly:assembly (-DskipTests=true) from main project directory.

infinispan-odata-server-1.0-SNAPSHOT.jar file should by located in ./target folder now.

---------------------
3) Running the server
---------------------

java -jar ./target/infinispan-odata-server-1.0-SNAPSHOT.jar http://localhost:8887/ODataInfinispanEndpoint.svc/ infinispan-dist.xml

The first parameter is URI where the service will be started,
the second parameter is name of a Infinispan configuration file which will be used for starting Infinispan caches.

It can be infinispan-dist.xml or indexing-perf.xml -- already defined as examples in server's resources,
 or an absolute path for custom xml Infinispan configuration file can be passed.

You should see console message:

"Infinispan OData server successfully started.

Service is listening at: http://localhost:8887/ODataInfinispanEndpoint.svc/

Metadata document is ready for access at: http://localhost:8887/ODataInfinispanEndpoint.svc/$metadata" (EDM in XML format)

Other suggested options:
-Xms512m -Xmx512m
-Djava.net.preferIPv4Stack=true
-Dlog4j.configuration=file:///path/to/log4j.xml

Troubleshooting: Don't use infinispan-odata-server.jar, use infinispan-odata-server-1.0-SNAPSHOT.jar instead.

For stopping the server use CTRL+C, or just find its ID and kill it.

-------------
4) Interface
------------

Consumers uses OData as a protocol for communicating with Infinispan OData server (producer).

The service exposes metadata document which describes defined service operations.

Accessible at: http://localhost:8887/ODataInfinispanEndpoint.svc/$metadata

http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_put?[options]

http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_get?[options]

http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_remove?[options]

http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_replace?[options]

NOTE: OData Entity sets, thus, caches, thus first parts of service operation names (i.e. odataCache) are CaSe SeNsItIvE.

Supported system query options:

$filter=\<expression\>

\<expression\> is built from OData query operators.

Supported OData query operators:

eq, and, or

NOTE: operators has to be used lowercase!

See next section for practical examples.

------------------------------
5) Practical usage examples
---------------------------

This section introduces basic client-server communication with Infinispan OData server.

CURL tool is used for a few demonstration examples -- it's not entirely user-friendly but the server is a middleware solution, not end-user GUI :)

Any HTTP client can be used as well.

Example CURL commands are intentionally "one-liners" to be prepared for direct copy and paste into console/terminal.


------------------------------
5a) Putting data into cache
---------------------------

JSON documents are put into caches using POST method with specified "application/json; charset=UTF-8" HTTP content-type header.

It is possible to set up IGNORE_RETURN_VALUES flag -- values are not being returned back to the client after put.

HTTP location header with URI for accessing just stored JSON document is returned after successful put.

(Server is supposed to started with infinispan-dist.xml or indexing-perf.xml)

Let's store some JSON documents into the odataCache cache:

HTTP "Content-Type", "application/json; charset=UTF-8" needs to be specified properly.

It is needed to enclose values ('person1') for function parameters (a --key-- in this case) within single quotation marks "'",
and moreover, to escape them "\'"

Put Neo and Trinity into the cache:

(The first put takes around 4 seconds because cache is being started.)

curl -X POST -H "Content-Type: application/json; charset=UTF-8" -d '{"id":"person1","name":"Neo","lastname":"Matrix"}' http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_put?key=\'person1\'

curl -X POST -H "Content-Type: application/json; charset=UTF-8" -d '{"id":"person2","name":"Trinity","lastname":"Matrix"}' http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_put?key=\'person2\'

stored values are returned immediately to the client.

And put Morpheus with IGNORE_RETURN_VALUES flag:

curl -X POST -H "Content-Type: application/json; charset=UTF-8" -d '{"id":"person3","name":"Morpheus","lastname":"Mc the coolest"}' http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_put?key=\'person3\'\&IGNORE_RETURN_VALUES=\'true\'


Now it's time to obtain heroes back from the cache.

------------------------------------
5b) Simple key-value based access
---------------------------------

HTTP header "Accept", "application/json; charset=UTF-8" needs to be specified properly.

curl -X GET -H "Accept: application/json; charset=UTF-8" http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_get?key=\'person1\'

Neo is returned ("d" stands for a "data"):

{ "d" : {"id":"person1","name":"Neo","lastname":"Matrix"}}


Now add agent Smith:

curl -X POST -H "Content-Type: application/json; charset=UTF-8" -d '{"id":"smith1","name":"Agent","lastname":"Smith"}' http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_put?key=\'smith1\'

And look at him:

curl -X GET -H "Accept: application/json; charset=UTF-8" http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_get?key=\'smith1\'

It is needed to replace him:

(NOTE: HTTP method PUT and appendix _replace is used in this case)

curl -X PUT -H "Content-Type: application/json; charset=UTF-8" -d '{"id":"smith1","name":"AgentXXX","lastname":"SmithXXX"}' http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_replace?key=\'smith1\'

New Smith agent should be returned:

{ "d" : {"id":"smith1","name":"AgentXXX","lastname":"SmithXXX"}}

We don't like him, so we will delete him, right?

(NOTE: HTTP method DELETE and appendix _remove is used, operation returns NO_CONTENT status)

curl -X DELETE -H "Accept: application/json; charset=UTF-8" http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_remove?key=\'smith1\'

Try to find him out!

curl -X GET -H "Accept: application/json; charset=UTF-8" http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_get?key=\'smith1\'

Entry was not found. -- yup he's gone :)

-------------------------------------------
5c) Document store (query based) access
---------------------------------------

HTTP "Accept", "application/json; charset=UTF-8" needs to be specified properly.

Pure OData query:

$filter=name eq 'Neo' or name eq 'Trinity

needs to be encoded and escaped properly (escape $, ', and encode " " to "%20"):

\$filter=name%20eq%20\'Neo\'%20or%20name%20eq%20\'Trinity\'


Now obtain JSON documents from document store using OData query operators:

Neo or Trinity:

curl -X GET -H "Accept: application/json;charset=UTF-8" http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_get?\$filter=name%20eq%20\'Neo\'%20or%20name%20eq%20\'Trinity\'

A collection of documents is returned:

{ "d" : [{"id":"person1","name":"Neo","lastname":"Matrix"},
        {"id":"person2","name":"Trinity","lastname":"Matrix"}]}

name Neo and lastname Matrix:

curl -X GET -H "Accept: application/json;charset=UTF-8" http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_get?\$filter=name%20eq%20\'Neo\'%20and%20lastname%20eq%20\'Matrix\'

Just Neo is returned:

{ "d" : {"id":"person1","name":"Neo","lastname":"Matrix"}}


$top and $skip query options can be appended to $filter option in order to return only a desired portion of results.

This filter query will return all three heroes:

NOTE: without encoded stuff, pure OData query looks like this

$filter=lastname eq 'Matrix' or name eq 'Morpheus'

curl -X GET -H "Accept: application/json;charset=UTF-8" http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_get?\$filter=lastname%20eq%20\'Matrix\'%20or%20name%20eq%20\'Morpheus\'


Now let's try out $top and $filter query options. They needs to be appended after $filter expression with use of &

Select only 2 top results:

curl -X GET -H "Accept: application/json;charset=UTF-8" http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_get?\$filter=lastname%20eq%20\'Matrix\'%20or%20name%20eq%20\'Morpheus\'\&\$top=2

Skip just the first result:

curl -X GET -H "Accept: application/json;charset=UTF-8" http://localhost:8887/ODataInfinispanEndpoint.svc/odataCache_get?\$filter=lastname%20eq%20\'Matrix\'%20or%20name%20eq%20\'Morpheus\'\&\$skip=1

Pure OData queries look like this:

$filter=lastname eq 'Matrix' or name eq 'Morpheus'&top=2

$filter=lastname eq 'Matrix' or name eq 'Morpheus'&skip=1

-----------------
OData standards
---------------

OData specification related:

Event -- HTTP Response code | special

Successful GET request -- 200

Entry not found -- 404

Entry created -- 201 | "location" header with link for getting the entry back | returns back whole stored JSON document

or

Entry created -- 201 (user IGNORE_RETURN_VALUES) | "location" header with link for getting the entry back |

returns back string "Entry created -- ready for access here: " + content of "location" header


Supported $filter query option with eq, and, or expression operators for querying JSON text fields.

Supported $top and $skip query options (can be appended to $filter option to select only a potion of results)

Collections of JSON documents can be returned, general format:

{ d“ : [{ ... }, { ... }, { ... }]}

