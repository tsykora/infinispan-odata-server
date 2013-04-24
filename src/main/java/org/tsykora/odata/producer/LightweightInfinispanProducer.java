package org.tsykora.odata.producer;

import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OExtension;
import org.odata4j.core.OFunctionParameter;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmFunctionImport;
import org.odata4j.producer.BaseResponse;
import org.odata4j.producer.CountResponse;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityIdResponse;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.edm.MetadataProducer;

import java.util.Map;

/**
 * This is lightweight infinispan OData producer which implements ODataProducer interface.
 * This class contains only totally necessary methods and elements for being able to successfully
 * ack as a OData producer connected directly to in memory cache instance.
 *
 * It is able to put (create) entries in cache, update (merge? update?) entries in cache, delete (delete) entries from cache.
 *
 * DESIGN:
 *
 * While starting service (or once some consumer wants to interact?) it should be possible to pass
 * cache configuration builder instance here and start in memory cache according to this configuration.
 *
 * Then, this cache is the main storage for data.
 *
 * It is serving/producing cache entries as a ODataEntities in Responses.entity(>"OEntityInstanceContainingAllNecessaryData"<)
 * When creating entries (= putting into the cache) it is receiving OEntity too.
 * These OEntities bundles specific cache entries.
 *
 * Some handler, visitor, convertor (OEntity -> CacheEntry -> OEntity) will be needed.
 * OData is able to transfer data as a OEntities and everything which is needed (cache entry data) has to be
 * packed inside of it. (Consider using some EntityDataModel (EDM) etc.)
 *
 * @author tsykora
 */
public class LightweightInfinispanProducer implements ODataProducer {


   /**
    * Obtains the service metadata for this producer.
    *
    * @return a fully-constructed metadata object
    */
   @Override
   public EdmDataServices getMetadata() {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" +
                                                    " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Obtains the ODataProducer implementation that serves the metadata as OData EDM constructs.
    *
    * @return the MetadataProducer object
    */
   @Override
   public MetadataProducer getMetadataProducer() {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Gets all the entities for a given set matching the query information.
    *
    * @param entitySetName the entity-set name for entities to return
    * @param queryInfo     the additional constraints to apply to the entities
    * @return a packaged collection of entities to pass back to the client
    */
   @Override
   public EntitiesResponse getEntities(String entitySetName, QueryInfo queryInfo) {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Gets the count of all the entities for a given set matching the query information.
    *
    * @param entitySetName the entity-set name for entities whose count is returned
    * @param queryInfo     the additional constraints to apply to the entities
    * @return count of the entities
    */
   @Override
   public CountResponse getEntitiesCount(String entitySetName, QueryInfo queryInfo) {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Obtains a single entity based on its type and key.
    *
    * @param entitySetName the entity-set name for entities to return
    * @param entityKey     the unique entity-key of the entity to start with
    * @param queryInfo     the additional constraints applicable to single-entity queries
    * @return the resulting entity
    */
   @Override
   public EntityResponse getEntity(String entitySetName, OEntityKey entityKey, EntityQueryInfo queryInfo) {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Given a specific entity, follow one of its navigation properties, applying constraints as appropriate. Return the
    * resulting entity, entities, or property value.
    *
    * @param entitySetName the entity-set of the entity to start with
    * @param entityKey     the unique entity-key of the entity to start with
    * @param navProp       the navigation property to follow
    * @param queryInfo     additional constraints to apply to the result
    * @return the resulting entity, entities, or property value
    */
   @Override
   public BaseResponse getNavProperty(String entitySetName, OEntityKey entityKey, String navProp, QueryInfo queryInfo) {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Given a specific entity, follow one of its navigation properties, applying constraints as appropriate. Return the
    * count of the resulting entities.
    *
    * @param entitySetName the entity-set of the entity to start with
    * @param entityKey     the unique entity-key of the entity to start with
    * @param navProp       the navigation property to follow
    * @param queryInfo     additional constraints to apply to the result
    * @return the count of the resulting entities
    */
   @Override
   public CountResponse getNavPropertyCount(String entitySetName, OEntityKey entityKey, String navProp, QueryInfo queryInfo) {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Releases any resources managed by this producer.
    */
   @Override
   public void close() {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Creates a new OData entity.
    *
    * @param entitySetName the entity-set name
    * @param entity        the request entity sent from the client
    * @return the newly-created entity, fully populated with the key and default properties
    * @see <a href="http://www.odata.org/developers/protocols/operations#CreatingnewEntries">[odata.org] Creating new
    *      Entries</a>
    */
   @Override
   public EntityResponse createEntity(String entitySetName, OEntity entity) {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Creates a new OData entity as a reference of an existing entity, implicitly linked to the existing entity by a
    * navigation property.
    *
    * @param entitySetName the entity-set name of the existing entity
    * @param entityKey     the entity-key of the existing entity
    * @param navProp       the navigation property off of the existing entity
    * @param entity        the request entity sent from the client
    * @return the newly-created entity, fully populated with the key and default properties, and linked to the existing
    *         entity
    * @see <a href="http://www.odata.org/developers/protocols/operations#CreatingnewEntries">[odata.org] Creating new
    *      Entries</a>
    */
   @Override
   public EntityResponse createEntity(String entitySetName, OEntityKey entityKey, String navProp, OEntity entity) {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Deletes an existing entity.
    *
    * @param entitySetName the entity-set name of the entity
    * @param entityKey     the entity-key of the entity
    * @see <a href="http://www.odata.org/developers/protocols/operations#DeletingEntries">[odata.org] Deleting
    *      Entries</a>
    */
   @Override
   public void deleteEntity(String entitySetName, OEntityKey entityKey) {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Modifies an existing entity using merge semantics.
    *
    * @param entitySetName the entity-set name
    * @param entity        the entity modifications sent from the client
    * @see <a href="http://www.odata.org/developers/protocols/operations#UpdatingEntries">[odata.org] Updating
    *      Entries</a>
    */
   @Override
   public void mergeEntity(String entitySetName, OEntity entity) {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Modifies an existing entity using update semantics.
    *
    * @param entitySetName the entity-set name
    * @param entity        the entity modifications sent from the client
    * @see <a href="http://www.odata.org/developers/protocols/operations#UpdatingEntries">[odata.org] Updating
    *      Entries</a>
    */
   @Override
   public void updateEntity(String entitySetName, OEntity entity) {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Returns the value of an entity's navigation property as a collection of entity links (or a single link if the
    * association cardinality is 1).
    *
    * @param sourceEntity  an entity with at least one navigation property
    * @param targetNavProp the navigation property
    * @return a collection of entity links (or a single link if the association cardinality is 1)
    */
   @Override
   public EntityIdResponse getLinks(OEntityId sourceEntity, String targetNavProp) {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Creates a link between two entities.
    *
    * @param sourceEntity  an entity with at least one navigation property
    * @param targetNavProp the navigation property
    * @param targetEntity  the link target entity
    * @see <a href="http://www.odata.org/developers/protocols/operations#CreatingLinksbetweenEntries">[odata.org] Creating
    *      Links between Entries</a>
    */
   @Override
   public void createLink(OEntityId sourceEntity, String targetNavProp, OEntityId targetEntity) {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Replaces an existing link between two entities.
    *
    * @param sourceEntity       an entity with at least one navigation property
    * @param targetNavProp      the navigation property
    * @param oldTargetEntityKey if the navigation property represents a set, the key identifying the old target entity
    *                           within the set, else n/a
    * @param newTargetEntity    the new link target entity
    * @see <a href="http://www.odata.org/developers/protocols/operations#ReplacingLinksbetweenEntries">[odata.org]
    *      Replacing Links between Entries</a>
    */
   @Override
   public void updateLink(OEntityId sourceEntity, String targetNavProp, OEntityKey oldTargetEntityKey, OEntityId newTargetEntity) {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Deletes an existing link between two entities.
    *
    * @param sourceEntity    an entity with at least one navigation property
    * @param targetNavProp   the navigation property
    * @param targetEntityKey if the navigation property represents a set, the key identifying the target entity within the
    *                        set, else n/a
    */
   @Override
   public void deleteLink(OEntityId sourceEntity, String targetNavProp, OEntityKey targetEntityKey) {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Calls a function (aka Service Operation).
    *
    * @param name      the name of the function
    * @param params    the parameters to the function
    * @param queryInfo additional query parameters to apply to collection-valued results
    * @return a BaseResponse appropriately typed to hold the function results
    *         From the spec:<pre>
    *            The return type of &lt;Function&gt; MUST be one of the following:
    *                An EDMSimpleType or collection of EDMSimpleTypes.
    *                An entity type or collection of entity types.
    *                A complex type or collection of complex types.
    *                A row type or collection of row types.
    *                &lt;ReturnType&gt; can contain a maximum of one &lt;CollectionType&gt; element.
    *                &lt;ReturnType&gt; can contain a maximum of one &lt;ReferenceType&gt; element.
    *                &lt;ReturnType&gt; can contain a maximum of one &lt;RowType&gt; element.
    *                A ref type or collection of ref types.</pre>
    */
   @Override
   public BaseResponse callFunction(EdmFunctionImport name, Map<String, OFunctionParameter> params, QueryInfo queryInfo) {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }

   /**
    * Finds an extension instance given an interface, if one exists.
    *
    * @param clazz        the extension interface
    * @param <TExtension> type of extension
    * @return the extension instance, or null if no extension exists for this type
    */
   @Override
   public <TExtension extends OExtension<T>> TExtension findExtension(Class<TExtension> clazz) {
      throw new UnsupportedOperationException("LightweightInfinispanProducer does not support this method:" + " " + new Exception().getStackTrace()[0].getMethodName());
   }
}
