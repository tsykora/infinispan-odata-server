package org.tsykora.odata.producer;

import org.core4j.Enumerable;
import org.core4j.Func;
import org.core4j.Func1;
import org.core4j.Predicate1;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.odata4j.core.*;
import org.odata4j.edm.*;
import org.odata4j.exceptions.NotFoundException;
import org.odata4j.exceptions.NotImplementedException;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.expression.OrderByExpression.Direction;
import org.odata4j.producer.*;
import org.odata4j.producer.edm.MetadataProducer;
import org.odata4j.producer.inmemory.BeanBasedPropertyModel;
import org.odata4j.producer.inmemory.EntityIdFunctionPropertyModelDelegate;
import org.odata4j.producer.inmemory.EnumsAsStringsPropertyModelDelegate;
import org.odata4j.producer.inmemory.InMemoryComplexTypeInfo;
import org.odata4j.producer.inmemory.InMemoryEvaluation;
import org.odata4j.producer.inmemory.InMemoryTypeMapping;
import org.odata4j.producer.inmemory.PropertyModel;
import org.tsykora.odata.producer.InfinispanProducer2.RequestContext.RequestType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.odata4j.producer.inmemory.PropertyModelDelegate;
import org.odata4j.repack.org.apache.commons.codec.binary.Base64;

//import org.odata4j.producer.inmemory.InMemoryProducer.RequestContext.RequestType;
/**
 *
 * InMemoryProducer with implemented direct access to Infinispan Cache.
 *
 */
public class InfinispanProducer2 implements ODataProducer {

    private static final boolean DUMP = false;

    private static void dump(String msg) {
        if (DUMP) {
            System.out.println(msg);
        }
    }
    public static final String ID_PROPNAME = "EntityId";
    private final String namespace;
    private final String containerName;
    private final int maxResults;
    // preserve the order of registration
    private final Map<String, InMemoryEntityInfo<?>> eis = new LinkedHashMap<String, InMemoryEntityInfo<?>>();
    private final Map<String, InMemoryComplexTypeInfo<?>> complexTypes = new LinkedHashMap<String, InMemoryComplexTypeInfo<?>>();
    private EdmDataServices metadata;
    private final EdmDecorator decorator;
    private final MetadataProducer metadataProducer;
    private final InMemoryTypeMapping typeMapping;
    private boolean includeNullPropertyValues = true;
    private final boolean flattenEdm;
    private static final int DEFAULT_MAX_RESULTS = 100;
    // not static - cache instance is running with producer instance
    private Cache<Object, Object> ispnCache;

    /**
     * Creates a new instance of an in-memory POJO producer.
     *
     * @param namespace the namespace of the schema registrations
     */
    public InfinispanProducer2(String namespace) {
        this(namespace, DEFAULT_MAX_RESULTS);
    }

    /**
     * Creates a new instance of an in-memory POJO producer.
     *
     * @param namespace  the namespace of the schema registrations
     * @param maxResults the maximum number of entities to return in a single call
     */
    public InfinispanProducer2(String namespace, int maxResults) {
        this(namespace, null, maxResults, null, null);
    }

    /**
     * Creates a new instance of an in-memory POJO producer.
     *
     * @param namespace     the namespace of the schema registrations
     * @param containerName the container name for generated metadata
     * @param maxResults    the maximum number of entities to return in a single call
     * @param decorator     a decorator to use for edm customizations
     * @param typeMapping   optional mapping between java types and edm types, null for default
     */
    public InfinispanProducer2(String namespace, String containerName, int maxResults, EdmDecorator decorator, InMemoryTypeMapping typeMapping) {
        this(namespace, containerName, maxResults, decorator, typeMapping,
                true); // legacy: flatten edm
    }

    public InfinispanProducer2(String namespace, String containerName, int maxResults, EdmDecorator decorator, InMemoryTypeMapping typeMapping,
            boolean flattenEdm) {
        this.namespace = namespace;
        this.containerName = containerName != null && !containerName.isEmpty() ? containerName : "Container";
        this.maxResults = maxResults;
        this.decorator = decorator;
        this.metadataProducer = new MetadataProducer(this, decorator);
        this.typeMapping = typeMapping == null ? InMemoryTypeMapping.DEFAULT : typeMapping;
        this.flattenEdm = flattenEdm;

        // Creating ISPN layer in producer constructor
        ispnCache = new DefaultCacheManager().getCache();
    }

    @Override
    public EdmDataServices getMetadata() {
        if (metadata == null) {
            metadata = newEdmGenerator(namespace, typeMapping, ID_PROPNAME, eis, complexTypes).generateEdm(decorator).build();
        }
        return metadata;
    }

    public String getContainerName() {
        return containerName;
    }

    protected InMemoryEdmGenerator newEdmGenerator(String namespace, InMemoryTypeMapping typeMapping, String idPropName, Map<String, InMemoryEntityInfo<?>> eis,
            Map<String, InMemoryComplexTypeInfo<?>> complexTypesInfo) {
        return new InMemoryEdmGenerator(namespace, containerName, typeMapping, ID_PROPNAME, eis, complexTypesInfo, this.flattenEdm);
    }

    @Override
    public MetadataProducer getMetadataProducer() {
        return metadataProducer;
    }

    @Override
    public void close() {
    }

    public void setIncludeNullPropertyValues(boolean value) {
        this.includeNullPropertyValues = value;
    }

    /**
     * Registers a POJO class as an EdmComplexType.
     *
     * @param complexTypeClass    The POJO Class
     * @param typeName            The name of the EdmComplexType
     */
//   public <TEntity> void registerComplexType(Class<TEntity> complexTypeClass, String typeName) {
//      registerComplexType(complexTypeClass, typeName,
//                          new EnumsAsStringsPropertyModelDelegate(new BeanBasedPropertyModel(complexTypeClass, this.flattenEdm)));
//   }
//   public <TEntity> void registerComplexType(Class<TEntity> complexTypeClass, String typeName, PropertyModel propertyModel) {
//      InMemoryComplexTypeInfo<TEntity> i = new InMemoryComplexTypeInfo<TEntity>();
//      i.typeName = (typeName == null) ? complexTypeClass.getSimpleName() : typeName;
//      i.entityClass = complexTypeClass;
//      i.propertyModel = propertyModel;
//
//      complexTypes.put(i.typeName, i);
//      metadata = null;
//   }
    /**
     * Registers a new entity based on a POJO, with support for composite keys.
     *
     * @param entityClass   the class of the entities that are to be stored in the set
     * @param entitySetName the alias the set will be known by; this is what is used in the OData url
     * @param get           a function to iterate over the elements in the set
     * @param keys          one or more keys for the entity
     */
    public <TEntity> void register(Class<TEntity> entityClass, String entitySetName, Func<Iterable<TEntity>> get, String... keys) {
        register(entityClass, entitySetName, entitySetName, get, keys);
    }

    /**
     * Registers a new entity based on a POJO, with support for composite keys.
     *
     * @param entityClass    the class of the entities that are to be stored in the set
     * @param entitySetName  the alias the set will be known by; this is what is used in the OData url
     * @param entityTypeName type name of the entity
     * @param get            a function to iterate over the elements in the set
     * @param keys           one or more keys for the entity
     */
    public <TEntity> void register(Class<TEntity> entityClass, String entitySetName,
            String entityTypeName, Func<Iterable<TEntity>> get, String... keys) {
        PropertyModel model = new BeanBasedPropertyModel(entityClass, this.flattenEdm);
        model = new EnumsAsStringsPropertyModelDelegate(model);
        register(entityClass, model, entitySetName, entityTypeName, get, keys);
    }

    /**
     * Registers a new entity set based on a POJO type using the default property model.
     */
    public <TEntity, TKey> void register(Class<TEntity> entityClass, Class<TKey> keyClass,
            String entitySetName, Func<Iterable<TEntity>> get, Func1<TEntity, TKey> id) {
        PropertyModel model = new BeanBasedPropertyModel(entityClass, this.flattenEdm);
        
        model = new EnumsAsStringsPropertyModelDelegate(model);
        model = new EntityIdFunctionPropertyModelDelegate<TEntity, TKey>(model, ID_PROPNAME, keyClass, id);
        register(entityClass, model, entitySetName, get, ID_PROPNAME);
    }

    /**
     * Registers a new entity set based on a POJO type and a property model.
     *
     * @param entityClass   the class of the entities that are to be stored in the set
     * @param propertyModel a way to get/set properties on the POJO
     * @param entitySetName the alias the set will be known by; this is what is used in the ODATA URL
     * @param get           a function to iterate over the elements in the set
     * @param keys          one or more keys for the entity
     */
    public <TEntity, TKey> void register(
            Class<TEntity> entityClass,
            PropertyModel propertyModel,
            String entitySetName,
            Func<Iterable<TEntity>> get,
            String... keys) {
        register(entityClass, propertyModel, entitySetName, entitySetName, get, keys);
    }

    public <TEntity> void register(
            final Class<TEntity> entityClass,
            final PropertyModel propertyModel,
            final String entitySetName,
            final String entityTypeName,
            final Func<Iterable<TEntity>> get,
            final String... keys) {
        register(entityClass, propertyModel, entitySetName, entityTypeName,
                get, null, keys);
    }

    public <TEntity> void register(
            final Class<TEntity> entityClass,
            final PropertyModel propertyModel,
            final String entitySetName,
            final String entityTypeName,
            final Func<Iterable<TEntity>> get,
            final Func1<RequestContext, Iterable<TEntity>> getWithContext,
            final String... keys) {

        InMemoryEntityInfo<TEntity> ei = new InMemoryEntityInfo<TEntity>();
        ei.entitySetName = entitySetName;
        ei.entityTypeName = entityTypeName;
        ei.properties = propertyModel;
        ei.get = get;
        ei.getWithContext = getWithContext;
        ei.keys = keys;
        ei.entityClass = entityClass;
        ei.hasStream = OAtomStreamEntity.class.isAssignableFrom(entityClass);

        ei.id = new Func1<Object, HashMap<String, Object>>() {

            @Override
            public HashMap<String, Object> apply(Object input) {
                HashMap<String, Object> values = new HashMap<String, Object>();
                for (String key : keys) {
                    values.put(key, eis.get(entitySetName).properties.getPropertyValue(input, key));
                }
                return values;
            }
        };

        eis.put(entitySetName, ei);
        metadata = null;
    }

    protected InMemoryComplexTypeInfo<?> findComplexTypeInfoForClass(Class<?> clazz) {
        for (InMemoryComplexTypeInfo<?> typeInfo : this.complexTypes.values()) {
            if (typeInfo.getEntityClass().equals(clazz)) {
                return typeInfo;
            }
        }

        return null;
    }

    protected InMemoryEntityInfo<?> findEntityInfoForClass(Class<?> clazz) {
        for (InMemoryEntityInfo<?> typeInfo : this.eis.values()) {
            if (typeInfo.getEntityClass().equals(clazz)) {
                return typeInfo;
            }
        }

        return null;
    }

    protected InMemoryEntityInfo<?> findEntityInfoForEntitySet(String entitySetName) {
        for (InMemoryEntityInfo<?> typeInfo : this.eis.values()) {
            if (typeInfo.getEntitySetName().equals(entitySetName)) {
                return typeInfo;
            }
        }

        return null;
    }

    /**
     * Transforms a POJO into a list of OProperties based on a given EdmStructuralType.
     *
     * @param obj            the POJO to transform
     * @param propertyModel  the PropertyModel to use to access POJO class structure and values.
     * @param structuralType the EdmStructuralType
     * @param properties     put properties into this list.
     */
    protected void addPropertiesFromObject(Object obj, PropertyModel propertyModel, EdmStructuralType structuralType, List<OProperty<?>> properties, PropertyPathHelper pathHelper) {
        dump("addPropertiesFromObject: " + obj.getClass().getName());
        for (Iterator<EdmProperty> it = structuralType.getProperties().iterator(); it.hasNext();) {
            EdmProperty property = it.next();

            // $select projections not allowed for complex types....hmmh...why?
            if (structuralType instanceof EdmEntityType && !pathHelper.isSelected(property.getName())) {
                continue;
            }

            Object value = propertyModel.getPropertyValue(obj, property.getName());
            dump("  prop: " + property.getName() + " val: " + value);
            if (value == null && !this.includeNullPropertyValues) {
                // this is not permitted by the spec but makes debugging wide entity types
                // much easier.
                continue;
            }

            if (property.getCollectionKind() == EdmProperty.CollectionKind.NONE) {
                if (property.getType().isSimple()) {
                    properties.add(OProperties.simple(property.getName(), (EdmSimpleType) property.getType(), value));
                } else {
                    // complex.
                    if (value == null) {
                        properties.add(OProperties.complex(property.getName(), (EdmComplexType) property.getType(), null));
                    } else {
                        Class<?> propType = propertyModel.getPropertyType(property.getName());
                        InMemoryComplexTypeInfo<?> typeInfo = findComplexTypeInfoForClass(propType);
                        if (typeInfo == null) {
                            continue;
                        }
                        List<OProperty<?>> cprops = new ArrayList<OProperty<?>>();
                        addPropertiesFromObject(value, typeInfo.getPropertyModel(), (EdmComplexType) property.getType(), cprops, pathHelper);
                        properties.add(OProperties.complex(property.getName(), (EdmComplexType) property.getType(), cprops));
                    }
                }
            } else {
                // collection.
                Iterable<?> values = propertyModel.getCollectionValue(obj, property.getName());
                OCollection.Builder<OObject> b = OCollections.newBuilder(property.getType());
                if (values != null) {
                    Class<?> propType = propertyModel.getCollectionElementType(property.getName());
                    InMemoryComplexTypeInfo<?> typeInfo = property.getType().isSimple() ? null : findComplexTypeInfoForClass(propType);
                    if ((!property.getType().isSimple()) && typeInfo == null) {
                        continue;
                    }
                    for (Object v : values) {
                        if (property.getType().isSimple()) {
                            b.add(OSimpleObjects.create((EdmSimpleType) property.getType(), v));
                        } else {
                            List<OProperty<?>> cprops = new ArrayList<OProperty<?>>();
                            addPropertiesFromObject(v, typeInfo.getPropertyModel(), (EdmComplexType) property.getType(), cprops, pathHelper);
                            b.add(OComplexObjects.create((EdmComplexType) property.getType(), cprops));
                        }
                    }
                }
                properties.add(OProperties.collection(property.getName(),
                        // hmmmh...is something is wrong here if I have to create a new EdmCollectionType?
                        new EdmCollectionType(EdmProperty.CollectionKind.Collection,
                        property.getType()), b.build()));
            }
        }
        dump("done addPropertiesFromObject: " + obj.getClass().getName());
    }

    protected OEntity toOEntity(EdmEntitySet ees, Object obj, PropertyPathHelper pathHelper) {

        InMemoryEntityInfo<?> ei = this.findEntityInfoForClass(obj.getClass()); //  eis.get(ees.getName());
        final List<OLink> links = new ArrayList<OLink>();
        final List<OProperty<?>> properties = new ArrayList<OProperty<?>>();

        Map<String, Object> keyKVPair = new HashMap<String, Object>();
        for (String key : ei.getKeys()) {
            Object keyValue = ei.getPropertyModel().getPropertyValue(obj, key);
            keyKVPair.put(key, keyValue);
        }

        // the entity set being queried may contain objects of subtypes of the entity set's type
        EdmEntityType edmEntityType = (EdmEntityType) this.getMetadata().findEdmEntityType(namespace + "." + ei.getEntityTypeName());

        // "regular" properties
        addPropertiesFromObject(obj, ei.getPropertyModel(), edmEntityType, properties, pathHelper);

        // navigation properties

        for (final EdmNavigationProperty navProp : edmEntityType.getNavigationProperties()) {

            if (!pathHelper.isSelected(navProp.getName())) {
                continue;
            }

            if (!pathHelper.isExpanded(navProp.getName())) {
                // defer
                if (navProp.getToRole().getMultiplicity() == EdmMultiplicity.MANY) {
                    links.add(OLinks.relatedEntities(null, navProp.getName(), null));
                } else {
                    links.add(OLinks.relatedEntity(null, navProp.getName(), null));
                }
            } else {
                // inline
                pathHelper.navigate(navProp.getName());
                if (navProp.getToRole().getMultiplicity() == EdmMultiplicity.MANY) {
                    List<OEntity> relatedEntities = new ArrayList<OEntity>();

                    EdmEntitySet relEntitySet = null;

                    for (final Object entity : getRelatedPojos(navProp, obj, ei)) {
                        if (relEntitySet == null) {
                            InMemoryEntityInfo<?> oei = this.findEntityInfoForClass(entity.getClass());
                            relEntitySet = getMetadata().getEdmEntitySet(oei.getEntitySetName());
                        }

                        relatedEntities.add(toOEntity(relEntitySet, entity, pathHelper));
                    }

                    // relation and href will be filled in later for atom or json
                    links.add(OLinks.relatedEntitiesInline(null, navProp.getName(), null, relatedEntities));
                } else {
                    final Object entity = ei.getPropertyModel().getPropertyValue(obj, navProp.getName());
                    OEntity relatedEntity = null;

                    if (entity != null) {
                        InMemoryEntityInfo<?> oei = this.findEntityInfoForClass(entity.getClass());
                        EdmEntitySet relEntitySet = getMetadata().getEdmEntitySet(oei.getEntitySetName());
                        relatedEntity = toOEntity(relEntitySet, entity, pathHelper);
                    }
                    links.add(OLinks.relatedEntityInline(null, navProp.getName(), null, relatedEntity));
                }

                pathHelper.popPath();
            }
        }

        return OEntities.create(ees, edmEntityType, OEntityKey.create(keyKVPair), properties, links, obj);
    }

    protected Iterable<?> getRelatedPojos(EdmNavigationProperty navProp, Object srcObject, InMemoryEntityInfo<?> srcInfo) {
        if (navProp.getToRole().getMultiplicity() == EdmMultiplicity.MANY) {
            Iterable<?> i = srcInfo.getPropertyModel().getCollectionValue(srcObject, navProp.getName());
            return i == null ? Collections.EMPTY_LIST : i;
        } else {
            // can be null
            return Collections.singletonList(srcInfo.getPropertyModel().getPropertyValue(srcObject, navProp.getName()));
        }
    }

    private static Predicate1<Object> filterToPredicate(final BoolCommonExpression filter, final PropertyModel properties) {
        return new Predicate1<Object>() {

            public boolean apply(Object input) {
                return InMemoryEvaluation.evaluate(filter, input, properties);
            }
        };
    }

    /**
     * Is returning all entries from local cache.
     * 
     * @param entitySetName - cache name
     * @param queryInfo - other special restrictions??
     * @return 
     */
    @Override
    public EntitiesResponse getEntities(String entitySetName, final QueryInfo queryInfo) {

        // go for entries directly into cache

        List<Object> entriesObjects = new ArrayList<Object>();

        for (Object cacheEntryKey : ispnCache.keySet()) {
            // get all entries from cache and return it as objects
            RequestContext rc =
                    RequestContext.newBuilder(RequestType.GetEntity).entitySetName(entitySetName).
                    entitySet(getMetadata().getEdmEntitySet(entitySetName)).
                    queryInfo(queryInfo).ispnCacheKey(cacheEntryKey).build();

            final Object rt = getEntityPojo(rc);

            entriesObjects.add(rt);
        }

        Enumerable<Object> objects = Enumerable.create(entriesObjects);


        final RequestContext rc = RequestContext.newBuilder(RequestType.GetEntities).
                entitySetName(entitySetName).entitySet(getMetadata().
                getEdmEntitySet(entitySetName)).queryInfo(queryInfo).
                pathHelper(new PropertyPathHelper(queryInfo)).build();

        final InMemoryEntityInfo<?> ei = eis.get(entitySetName);
//
//        Enumerable<Object> objects = ei.getWithContext == null
//                ? Enumerable.create(ei.get.apply()).cast(Object.class)
//                : Enumerable.create(ei.getWithContext.apply(rc)).cast(Object.class);


        return getEntitiesResponse(rc, rc.getEntitySet(), objects, ei.getPropertyModel());
    }

    protected EntitiesResponse getEntitiesResponse(final RequestContext rc, final EdmEntitySet targetEntitySet, Enumerable<Object> objects, PropertyModel propertyModel) {
        // apply filter
        final QueryInfo queryInfo = rc.getQueryInfo();
        if (queryInfo != null && queryInfo.filter != null) {
            objects = objects.where(filterToPredicate(queryInfo.filter, propertyModel));
        }

        // compute inlineCount, must be done after applying filter
        Integer inlineCount = null;
        if (queryInfo != null && queryInfo.inlineCount == InlineCount.ALLPAGES) {
            objects = Enumerable.create(objects.toList()); // materialize up front, since we're about to count
            inlineCount = objects.count();
        }

        // apply ordering
        if (queryInfo != null && queryInfo.orderBy != null) {
            objects = orderBy(objects, queryInfo.orderBy, propertyModel);
        }

        // work with oentities
        Enumerable<OEntity> entities = objects.select(new Func1<Object, OEntity>() {

            @Override
            public OEntity apply(Object input) {
                return toOEntity(targetEntitySet, input, rc.getPathHelper());
            }
        });

        // skip records by $skipToken
        if (queryInfo != null && queryInfo.skipToken != null) {
            final Boolean[] skipping = new Boolean[]{true};
            entities = entities.skipWhile(new Predicate1<OEntity>() {

                @Override
                public boolean apply(OEntity input) {
                    if (skipping[0]) {
                        String inputKey = input.getEntityKey().toKeyString();
                        if (queryInfo.skipToken.equals(inputKey)) {
                            skipping[0] = false;
                        }
                        return true;
                    }
                    return false;
                }
            });
        }

        // skip records by $skip amount
        if (queryInfo != null && queryInfo.skip != null) {
            entities = entities.skip(queryInfo.skip);
        }

        // apply limit
        int limit = this.maxResults;
        if (queryInfo != null && queryInfo.top != null && queryInfo.top < limit) {
            limit = queryInfo.top;
        }
        entities = entities.take(limit + 1);

        // materialize OEntities
        List<OEntity> entitiesList = entities.toList();

        // determine skipToken if necessary
        String skipToken = null;
        if (entitiesList.size() > limit) {
            entitiesList = Enumerable.create(entitiesList).take(limit).toList();
            skipToken = entitiesList.size() == 0 ? null : Enumerable.create(entitiesList).last().getEntityKey().toKeyString();
        }

        return Responses.entities(entitiesList, targetEntitySet, inlineCount, skipToken);

    }

    @Override
    public CountResponse getEntitiesCount(String entitySetName, final QueryInfo queryInfo) {

        
        final RequestContext rc = RequestContext.newBuilder(RequestType.GetEntitiesCount).
                entitySetName(entitySetName).entitySet(getMetadata().getEdmEntitySet(entitySetName)).queryInfo(queryInfo).build();

        final InMemoryEntityInfo<?> ei = eis.get(entitySetName);

        final PropertyPathHelper pathHelper = new PropertyPathHelper(queryInfo);

        Enumerable<Object> objects = ei.getWithContext == null
                ? Enumerable.create(ei.get.apply()).cast(Object.class)
                : Enumerable.create(ei.getWithContext.apply(rc)).cast(Object.class);

        // apply filter
        if (queryInfo != null && queryInfo.filter != null) {
            objects = objects.where(filterToPredicate(queryInfo.filter, ei.properties));
        }

        // inlineCount is not applicable to $count queries
        if (queryInfo != null && queryInfo.inlineCount == InlineCount.ALLPAGES) {
            throw new UnsupportedOperationException("$inlinecount cannot be applied to the resource segment '$count'");
        }

        // ignore ordering for count

        // work with oentities.
        Enumerable<OEntity> entities = objects.select(new Func1<Object, OEntity>() {

            @Override
            public OEntity apply(Object input) {
                return toOEntity(rc.getEntitySet(), input, pathHelper);
            }
        });

        // skipToken is not applicable to $count queries
        if (queryInfo != null && queryInfo.skipToken != null) {
            throw new UnsupportedOperationException("Skip tokens can only be provided for requests that return collections of entities.");
        }

        // skip records by $skip amount
        // http://services.odata.org/Northwind/Northwind.svc/Customers/$count/?$skip=5
        if (queryInfo != null && queryInfo.skip != null) {
            entities = entities.skip(queryInfo.skip);
        }

        // apply $top.  maxResults is not applicable to $count but $top is.
        // http://services.odata.org/Northwind/Northwind.svc/Customers/$count/?$top=55
        int limit = Integer.MAX_VALUE;
        if (queryInfo != null && queryInfo.top != null && queryInfo.top < limit) {
            limit = queryInfo.top;
        }
        entities = entities.take(limit);

        return Responses.count(entities.count());
    }

    private Enumerable<Object> orderBy(Enumerable<Object> iter, List<OrderByExpression> orderBys, final PropertyModel properties) {
        for (final OrderByExpression orderBy : Enumerable.create(orderBys).reverse()) {
            iter = iter.orderBy(new Comparator<Object>() {

                @SuppressWarnings({"unchecked", "rawtypes"})
                public int compare(Object o1, Object o2) {
                    Comparable lhs = (Comparable) InMemoryEvaluation.evaluate(orderBy.getExpression(), o1, properties);
                    Comparable rhs = (Comparable) InMemoryEvaluation.evaluate(orderBy.getExpression(), o2, properties);
                    return (orderBy.getDirection() == Direction.ASCENDING ? 1 : -1) * lhs.compareTo(rhs);
                }
            });
        }
        return iter;
    }

    
    /**
     * If there is a getEntity() call on consumer then this getEntity() method on producer is called
     */
    @Override
    public EntityResponse getEntity(final String entitySetName, final OEntityKey entityKey, final EntityQueryInfo queryInfo) {

        PropertyPathHelper pathHelper = new PropertyPathHelper(queryInfo);

        // should be unchanged
        String ispnCacheKey = (String) entityKey.asSingleValue();

        // now build request context to include exact and right key for internal cache entry
        RequestContext rc =
                RequestContext.newBuilder(RequestType.GetEntity).entitySetName(entitySetName).
                entitySet(getMetadata().getEdmEntitySet(entitySetName)).
                entityKey(entityKey).queryInfo(queryInfo).pathHelper(pathHelper).ispnCacheKey(ispnCacheKey).build();

        final Object rt = getEntityPojo(rc);
        if (rt == null) {
            throw new NotFoundException("No entity found in entityset " + entitySetName
                    + " for key " + entityKey.toKeyStringWithoutParentheses()
                    + " and query info " + queryInfo);
        }

        OEntity oe = toOEntity(rc.getEntitySet(), rt, rc.getPathHelper());

        return Responses.entity(oe);
    }

    @Override
    public void mergeEntity(String entitySetName, OEntity entity) {
        // merge - what is equal to merge in ISPN?
        throw new NotImplementedException();
    }

    @Override
    public void updateEntity(String entitySetName, OEntity entity) {
        // simple update entry and re-call
        throw new NotImplementedException();
    }

    @Override
    public void deleteEntity(String entitySetName, OEntityKey entityKey) {
        // simple remove entry and re-call 
        throw new NotImplementedException();
    }

    @Override
    public EntityResponse createEntity(String entitySetName, final OEntity entity) {

        // Is put into IspnCache (or other cache - depends on entitySetName = cacheName)
        // cacheName can be defined by other was - EDM - later? If needed. Accept builders?
        // creating caches on the server (Still Jersey?)

        // IMPORTANT
        // don't need to register (registering is only for building proper EDM now) -- just put into cache
        // and getEntity() will discover new state of cache (with that new put already inside)


        System.out.println("\n\nI am in the createEntity method.....\n\n");

// <editor-fold defaultstate="collapsed" desc="old code">
        // pass entity here
        // register it
        // and return it back as entity response

        // need proper entity key and entitySetName
        // first pass via OData from consumer, register into memory and then get it via OData with proper http responses etc.

//      try {


//      // TODO do this transformation in some aggregate method (using some EDM model for it?) To transfer OEntity to Pojos?
//      InMemoryProducerExample.MyInternalCacheEntry entry = new
//            InMemoryProducerExample.MyInternalCacheEntry((String) entity.getProperty("Key").getValue(),
//                                                         (String) entity.getProperty("Value").getValue());
//
//      // TODO: why I am deleting all entries which were in that entity set?
//
//
//      final Set<InMemoryProducerExample.MyInternalCacheEntry> setOfEntries = new HashSet<InMemoryProducerExample.MyInternalCacheEntry>();
//
//      for (OEntity oe : getEntities(entitySetName, entityQueryInfoGlobal).getEntities()) {
//
//         InMemoryProducerExample.MyInternalCacheEntry oldEntry = new
//               InMemoryProducerExample.MyInternalCacheEntry((String) oe.getProperty("Key").getValue(),
//                                                            (String) oe.getProperty("Value").getValue());
//
//         setOfEntries.add(oldEntry);
//      }
//
//
////         setOfEntries.add(new InMemoryProducerExample.MyInternalCacheEntry("key66", "value66"));
////         setOfEntries.add(new InMemoryProducerExample.MyInternalCacheEntry("key77", "value77"));
////         setOfEntries.add(new InMemoryProducerExample.MyInternalCacheEntry("key88", "value88"));
//
//      // And ADD NEW ENTRY HERE INTO SET and register all
//      setOfEntries.add(entry);
//
//
//      System.out.println("About to register new entry into entitySetName: " + entitySetName);
//      System.out.println("Entry was transfered to pojo: key:" + entry.getKey() + " value: " + entry.getValue());
//      // TODO: REGISTER entity immediately from OEntity -- do some processing in register method
//      // TODO: rename register to sume put? I don't know
//
//
//      // Store into memory (will be instance cache, which will be configured by configuration sent via OData too in init phase)
//      // new entries are registered here: http://localhost:8887/InMemoryProducerExample.svc/CacheEntriesNew
//      // TODO: implement register function for one entry and this use for some kind of batching
//      register(InMemoryProducerExample.MyInternalCacheEntry.class, InMemoryProducerExample.MyInternalCacheEntry.class, entitySetName,
//               new Func<Iterable<InMemoryProducerExample.MyInternalCacheEntry>>() {
//                  public Iterable<InMemoryProducerExample.MyInternalCacheEntry> apply() {
//                     return setOfEntries;
//                  }
//               }, Funcs.method(InMemoryProducerExample.MyInternalCacheEntry.class, InMemoryProducerExample.MyInternalCacheEntry.class, "toString"));
//
//      System.out.println("New entry was successfully registered!!!");


//
//      // Some more logic
//      // connect it to the cache / cache store etc., this cache (or cache manager needs to be in some ('general') context then
//
//      // TODO: do I need to set some status, some response, correctly? !!!
//
//      return new EntityResponse() {
//         @Override
//         public OEntity getEntity() {
//            return entity;
//         }
//      };
//
////      throw new NotImplementedException();
        // TODO: what to return, for which object to set status response to 200 OK, to not throw exception?


        // TODO
        // pass entity here
        // register it
        // and return it back as entity response

        // need proper entity key and entitySetName

        // normally call GET ENTITY here with returning RESPONSE
        // pass proper parameters


//      QueryInfo queryInfo = entityQueryInfoGlobal;
//
//      // and set entity set name as set into which is entry stored newcacheentries.
//
//      PropertyPathHelper pathHelper = new PropertyPathHelper(queryInfo);
//
//      RequestContext rc = RequestContext.newBuilder(RequestType.GetEntity)
//            .entitySetName(entitySetName)
//            .entitySet(getMetadata()
//                             .getEdmEntitySet(entitySetName))
//            .entityKey(entity.getEntityKey())
//            .queryInfo(queryInfo)
//            .pathHelper(pathHelper).build();
//
//      final Object rt = getEntityPojo(rc);
//      if (rt == null)
//         throw new NotFoundException("No entity found in entityset " + entitySetName
//                                           + " for key " + entity.getEntityKey().toKeyStringWithoutParentheses()
//                                           + " and query info " + queryInfo);
//
//      OEntity oe = toOEntity(rc.getEntitySet(), rt, rc.getPathHelper());
        // it returned status CREATED succesfully  (status 201)

// </editor-fold>

        // TODO: properly handle abstract object into cache and cache keys
        // TODO: some handler? + <Object, Object> cache
        try {
            ispnCache.put(entity.getProperty("Key").getValue(), entity.getProperty("Value").getValue());
            
        } catch (Exception e) {
            // Maybe properties were registered as "key" "value" instead of "Key" "Value"
            System.out.println("\n\n ******* WARNING ******* Properties names "
                    + "Key and Value are invalid. Trying key and value! ******** \n\n"
                    + "Maybe this is not enough and still failing -- DEBUG THIS --"
                    + "InfinispanProducer2.createEntity()");
            
            // SERIALIZATION OR DESERIALIZATION
            ispnCache.put(entity.getProperty("key").getValue(), entity.getProperty("value").getValue());            
        }
        

        OEntityKey oentityKey = entity.getEntityKey();

        if (entity.getEntityKey() == null) {
            // this is probably request from consumer and entityKey is not set        
            // there are set only necessary properties for creating new MyInternalCacheEntry instance there
            Map<String, Object> entityKeysValues = new HashMap<String, Object>();
            entityKeysValues.put("Key", entity.getProperty("Key").getValue());
            oentityKey = OEntityKey.create(entityKeysValues.values());
        }

        return getEntity(entitySetName, oentityKey, null);
    }

    @Override
    public EntityResponse createEntity(String entitySetName, OEntityKey entityKey, String navProp, OEntity entity) {
        System.out.println("THIS IS SECOND createEntity CALL and this is NOT IMPLEMENTED YET!!!");
        throw new NotImplementedException();
    }

    @Override
    public BaseResponse getNavProperty(String entitySetName, OEntityKey entityKey, String navProp, QueryInfo queryInfo) {

        RequestContext rc = RequestContext.newBuilder(RequestType.GetNavProperty).entitySetName(entitySetName).entitySet(getMetadata().getEdmEntitySet(entitySetName)).entityKey(entityKey).navPropName(navProp).queryInfo(queryInfo).pathHelper(new PropertyPathHelper(queryInfo)).build();

        EdmNavigationProperty navProperty = rc.getEntitySet().getType().findNavigationProperty(navProp);
        if (navProperty != null) {
            return getNavProperty(navProperty, rc);
        }

        // not a NavigationProperty:

        EdmProperty edmProperty = rc.getEntitySet().getType().findProperty(navProp);
        if (edmProperty == null) {
            throw new NotFoundException("Property " + navProp + " is not found");
        }
        // currently only simple types are supported
        EdmType edmType = edmProperty.getType();

        if (!edmType.isSimple()) {
            throw new NotImplementedException("Only simple types are supported. Property type is '" + edmType.getFullyQualifiedTypeName() + "'");
        }

        // get property value...
        InMemoryEntityInfo<?> entityInfo = eis.get(entitySetName);
        Object target = getEntityPojo(rc);
        Object propertyValue = entityInfo.properties.getPropertyValue(target, navProp);
        // ... and create OProperty
        OProperty<?> property = OProperties.simple(navProp, (EdmSimpleType<?>) edmType, propertyValue);

        return Responses.property(property);
    }

    protected EdmEntitySet findEntitySetForNavProperty(EdmNavigationProperty navProp) {
        EdmEntityType et = navProp.getToRole().getType();
        // assumes one set per type...
        for (EdmEntitySet set : this.getMetadata().getEntitySets()) {
            if (set.getType().equals(et)) {
                return set;
            }
        }
        return null;
    }

    /**
     * Gets the entity(s) on the target end of a NavigationProperty.
     *
     * @param navProp the navigation property
     * @param rc      the request context
     * @return a BaseResponse with either a single Entity (can be null) or a set of entities.
     */
    protected BaseResponse getNavProperty(EdmNavigationProperty navProp, RequestContext rc) {

        // First, get the source POJO.
        Object obj = getEntityPojo(rc);
        Iterable relatedPojos = this.getRelatedPojos(navProp, obj, this.findEntityInfoForClass(obj.getClass()));

        EdmEntitySet targetEntitySet = findEntitySetForNavProperty(navProp);

        if (navProp.getToRole().getMultiplicity() == EdmMultiplicity.MANY) {
            // apply filter, orderby, etc.
            return getEntitiesResponse(rc, targetEntitySet, Enumerable.create(relatedPojos), findEntityInfoForEntitySet(targetEntitySet.getName()).getPropertyModel());
        } else {
            return Responses.entity(this.toOEntity(targetEntitySet, relatedPojos.iterator().next(), rc.getPathHelper()));
        }
    }

    @Override
    public CountResponse getNavPropertyCount(String entitySetName, OEntityKey entityKey, String navProp, QueryInfo queryInfo) {
        throw new NotImplementedException();
    }

    @Override
    public EntityIdResponse getLinks(OEntityId sourceEntity, String targetNavProp) {
        throw new NotImplementedException();
    }

    @Override
    public void createLink(OEntityId sourceEntity, String targetNavProp, OEntityId targetEntity) {
        throw new NotImplementedException();
    }

    @Override
    public void updateLink(OEntityId sourceEntity, String targetNavProp, OEntityKey oldTargetEntityKey, OEntityId newTargetEntity) {
        throw new NotImplementedException();
    }

    @Override
    public void deleteLink(OEntityId sourceEntity, String targetNavProp, OEntityKey targetEntityKey) {
        throw new NotImplementedException();
    }

    @Override
    public BaseResponse callFunction(EdmFunctionImport name, Map<String, OFunctionParameter> params, QueryInfo queryInfo) {
        throw new NotImplementedException();
    }

    @Override
    public <TExtension extends OExtension<ODataProducer>> TExtension findExtension(Class<TExtension> clazz) {
        return null;
    }

    public static class RequestContext {

        public enum RequestType {

            GetEntity, GetEntities, GetEntitiesCount, GetNavProperty
        };
        public final RequestType requestType;
        private final String entitySetName;
        private EdmEntitySet entitySet;
        private final String navPropName;
        private final OEntityKey entityKey;
        private final QueryInfo queryInfo;
        private final PropertyPathHelper pathHelper;
        private final Object ispnCacheKey;

        public Object getIspnCacheKey() {            
            return ispnCacheKey;
        }

        public RequestType getRequestType() {
            return requestType;
        }

        public String getEntitySetName() {
            return entitySetName;
        }

        public EdmEntitySet getEntitySet() {
            return entitySet;
        }

        public String getNavPropName() {
            return navPropName;
        }

        public OEntityKey getEntityKey() {
            return entityKey;
        }

        public QueryInfo getQueryInfo() {
            return queryInfo;
        }

        public PropertyPathHelper getPathHelper() {
            return pathHelper;
        }

        public static Builder newBuilder(RequestType requestType) {
            return new Builder().requestType(requestType);
        }

        public static class Builder {

            private RequestType requestType;
            private String entitySetName;
            private EdmEntitySet entitySet;
            private String navPropName;
            private OEntityKey entityKey;
            private QueryInfo queryInfo;
            private PropertyPathHelper pathHelper;
            private Object ispnCacheKey;

            public Builder ispnCacheKey(Object value) {
                this.ispnCacheKey = value;
                return this;
            }

            public Builder requestType(RequestType value) {
                this.requestType = value;
                return this;
            }

            public Builder entitySetName(String value) {
                this.entitySetName = value;
                return this;
            }

            public Builder entitySet(EdmEntitySet value) {
                this.entitySet = value;
                return this;
            }

            public Builder navPropName(String value) {
                this.navPropName = value;
                return this;
            }

            public Builder entityKey(OEntityKey value) {
                this.entityKey = value;
                return this;
            }

            public Builder queryInfo(QueryInfo value) {
                this.queryInfo = value;
                return this;
            }

            public Builder pathHelper(PropertyPathHelper value) {
                this.pathHelper = value;
                return this;
            }

            public RequestContext build() {
                return new RequestContext(requestType, entitySetName, entitySet, navPropName, entityKey, queryInfo, pathHelper, ispnCacheKey);
            }
        }

        private RequestContext(RequestType requestType, String entitySetName, EdmEntitySet entitySet,
                String navPropName, OEntityKey entityKey, QueryInfo queryInfo, PropertyPathHelper pathHelper, Object ispnCacheKey) {
            this.requestType = requestType;
            this.entitySetName = entitySetName;
            this.entitySet = entitySet;
            this.navPropName = navPropName;
            this.entityKey = entityKey;
            this.queryInfo = queryInfo;
            this.pathHelper = pathHelper;
            this.ispnCacheKey = ispnCacheKey;
        }
    }

    /**
     * 
     * // - TODO - document THIS PROPERLY? Change in the future?
     * Given an entity set and an entity key, returns the pojo that is that entity instance. The default implementation
     * iterates over the entire set of pojos to find the desired instance.
     *
     * @param rc the current ReqeustContext, may be valuable to the ei.getWithContext impl
     * @return the pojo
     */
    @SuppressWarnings("unchecked")
    protected Object getEntityPojo(final RequestContext rc) {

        // I need to transfer cache entry to MyInternalCacheEntry
        // because this is Entity and I have EntityInfo about this class
        // RequestContext is my internal class here and it has rc.getEntityKey()

        // Citation EntityKey DOC: "The string representation of an entity-key is wrapped with parentheses" ('foo')

        // TODO - this operation with key will needs some general objects management (wildcards?)
        // TODO - cache should be general Object-Object too (or user specified and proper re-type here)
                
        InMemoryProducerExample.MyInternalCacheEntry mice = null;
        // entry exists?
        Object value = ispnCache.get(rc.getIspnCacheKey()); 
        if (value != null) {        
//            System.out.println("NEEEEEEEEED SERIALIZATION HEEEEEEEREEEEEEEEEEEEEEEE!!!!!!!!!! (or simply fix this after changes)");
//            mice = new InMemoryProducerExample.MyInternalCacheEntry(rc.getIspnCacheKey(), value);
            mice = new InMemoryProducerExample.MyInternalCacheEntry(new Base64(), new Base64());
        }
        return mice;
    }

    private enum TriggerType {

        Before, After
    };

    protected void fireUnmarshalEvent(Object pojo, OStructuralObject sobj, TriggerType ttype)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        try {
            Method m = pojo.getClass().getMethod(ttype == TriggerType.Before ? "beforeOEntityUnmarshal" : "afterOEntityUnmarshal", OStructuralObject.class);
            if (m != null) {
                m.invoke(pojo, sobj);
            }
        } catch (NoSuchMethodException ex) {
        }
    }

    /**
     * Transforms an OComplexObject into a POJO of the given class
     */
    public <T> T toPojo(OComplexObject entity, Class<T> pojoClass) throws InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        InMemoryComplexTypeInfo<?> e = this.findComplexTypeInfoForClass(pojoClass);

        T pojo = fillInPojo(entity, this.getMetadata().findEdmComplexType(
                this.namespace + "." + e.getTypeName()), e.getPropertyModel(), pojoClass);

        fireUnmarshalEvent(pojo, entity, TriggerType.After);
        return pojo;
    }

    /**
     * Populates a new POJO instance of type pojoClass using data from the given structural object.
     */
    protected <T> T fillInPojo(OStructuralObject sobj, EdmStructuralType stype, PropertyModel propertyModel,
            Class<T> pojoClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        T pojo = pojoClass.newInstance();
        fireUnmarshalEvent(pojo, sobj, TriggerType.Before);

        for (Iterator<EdmProperty> it = stype.getProperties().iterator(); it.hasNext();) {
            EdmProperty property = it.next();
            Object value = null;
            try {
                value = sobj.getProperty(property.getName()).getValue();
            } catch (Exception ex) {
                // property not define on object
                if (property.isNullable()) {
                    continue;
                } else {
                    throw new RuntimeException("missing required property " + property.getName());
                }
            }

            if (property.getCollectionKind() == EdmProperty.CollectionKind.NONE) {
                if (property.getType().isSimple()) {
                    // call the setter.
                    propertyModel.setPropertyValue(pojo, property.getName(), value);
                } else {
                    // complex.
                    // hmmh, value is a Collection<OProperty<?>>...why is it not an OComplexObject.

                    propertyModel.setPropertyValue(
                            pojo,
                            property.getName(),
                            value == null
                            ? null
                            : toPojo(
                            OComplexObjects.create((EdmComplexType) property.getType(), (List<OProperty<?>>) value),
                            propertyModel.getPropertyType(property.getName())));
                }
            } else {
                // collection.
                OCollection<? extends OObject> collection = (OCollection<? extends OObject>) value;
                List<Object> pojos = new ArrayList<Object>();
                for (OObject item : collection) {
                    if (collection.getType().isSimple()) {
                        pojos.add(((OSimpleObject) item).getValue());
                    } else {
                        // turn OComplexObject into a pojo
                        pojos.add(toPojo((OComplexObject) item, propertyModel.getCollectionElementType(property.getName())));
                    }
                }
                propertyModel.setCollectionValue(pojo, property.getName(), pojos);
            }
        }

        return pojo;
    }

    /*
     * Design note:
     * toPojo is functionality that is useful on both the producer and consumer side.
     * I'm putting it in the producer class for now although I suspect there is a
     * more elegant design that factors out POJO Classes and PropertyModels into
     * some kind of "PojoModelDefinition" class.  The producer side would then
     * layer and extended definition that defined how the PojoModelDefinition maps
     * to entity sets and such.
     *
     * with all that said, hopefully this start is useful.  I'm going to use it on
     * our producer side for now to handle createEntity payloads.
     */
    /**
     * Transforms the given entity into a POJO of type pojoClass.
     */
    public <T> T toPojo(OEntity entity, Class<T> pojoClass) throws InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        InMemoryEntityInfo<?> e = this.findEntityInfoForClass(pojoClass);

        // so, how is this going to work?
        // we have the PropertyModel available.  We can lookup the EdmStructuredType if necessary.

        EdmEntitySet entitySet = this.getMetadata().findEdmEntitySet(e.getEntitySetName());

        T pojo = fillInPojo(entity, entitySet.getType(), e.getPropertyModel(), pojoClass);

        // nav props
        for (Iterator<EdmNavigationProperty> it = entitySet.getType().getNavigationProperties().iterator(); it.hasNext();) {
            EdmNavigationProperty np = it.next();
            OLink link = null;
            try {
                link = entity.getLink(np.getName(), OLink.class);
            } catch (IllegalArgumentException nolinkex) {
                continue;
            }

            if (link.isInline()) {
                if (link.isCollection()) {
                    List<Object> pojos = new ArrayList<Object>();
                    for (OEntity relatedEntity : link.getRelatedEntities()) {
                        pojos.add(toPojo(relatedEntity, e.getPropertyModel().getCollectionElementType(np.getName())));
                    }
                    e.getPropertyModel().setCollectionValue(pojo, np.getName(), pojos);
                } else {
                    e.getPropertyModel().setPropertyValue(pojo, np.getName(),
                            toPojo(link.getRelatedEntity(), e.getPropertyModel().getPropertyType(np.getName())));
                }
            } // else ignore deferred links.
        }

        fireUnmarshalEvent(pojo, entity, TriggerType.After);
        return pojo;
    }

    // TODO: reuse this - everything is implemented and working
    // TODO: just use it and map it to the cache
    // TODO: re-implement this as some HANDLER for cache
    // TODO: this class will provide something like provider for accessing ISPN cache!
    public class InMemoryEntityInfo<TEntity> {

        // we are maintaining collection of these entities - they are mapped to EntitySetName in [eis] hash map
        // TODO: how about maintaining cache inside of instance of this class??
        // TODO: and directly access cache here
        String entitySetName;
        String entityTypeName;
        String[] keys;
        Class<TEntity> entityClass;
        Func<Iterable<TEntity>> get; //returning defined apply()
        Func1<RequestContext, Iterable<TEntity>> getWithContext;
        Func1<Object, HashMap<String, Object>> id;
        PropertyModel properties;
        boolean hasStream;

        public String getEntitySetName() {
            return entitySetName;
        }

        public String getEntityTypeName() {
            return entityTypeName;
        }

        public String[] getKeys() {
            return keys;
        }

        public Class<TEntity> getEntityClass() {
            return entityClass;
        }

        public Func<Iterable<TEntity>> getGet() {
            return get;
        }

        public Func1<RequestContext, Iterable<TEntity>> getGetWithContext() {
            System.out.println("Call from getGetWithContext method - return type was changed!!!");
            return getWithContext;
        }

        public Func1<Object, HashMap<String, Object>> getId() {
            return id;
        }

        public PropertyModel getPropertyModel() {
            return properties;
        }

        public boolean getHasStream() {
            return hasStream;
        }

        public Class<?> getSuperClass() {
            return entityClass.getSuperclass() != null && !entityClass.getSuperclass().equals(Object.class) ? entityClass.getSuperclass() : null;
        }
    }

    public class InMemoryEdmGenerator implements EdmGenerator {

        private static final boolean DUMP = false;
//      private static void dump(String msg) { if (DUMP) System.out.println(msg); }
        private final Logger log = Logger.getLogger(getClass().getName());
        private final String namespace;
        private final String containerName;
        protected final InMemoryTypeMapping typeMapping;
        protected final Map<String, InMemoryEntityInfo<?>> eis; // key: EntitySet name
        protected final Map<String, InMemoryComplexTypeInfo<?>> complexTypeInfo; // key complex type edm type name
        protected final List<EdmComplexType.Builder> edmComplexTypes = new ArrayList<EdmComplexType.Builder>();
        // Note, assumes each Java type will only have a single Entity Set defined for it.
        protected final Map<Class<?>, String> entitySetNameByClass = new HashMap<Class<?>, String>();
        // build these as we go now.
        protected Map<String, EdmEntityType.Builder> entityTypesByName = new HashMap<String, EdmEntityType.Builder>();
        protected Map<String, EdmEntitySet.Builder> entitySetsByName = new HashMap<String, EdmEntitySet.Builder>();
        protected final boolean flatten;

        public InMemoryEdmGenerator(String namespace, String containerName, InMemoryTypeMapping typeMapping,
                String idPropertyName, Map<String, InfinispanProducer2.InMemoryEntityInfo<?>> eis,
                Map<String, InMemoryComplexTypeInfo<?>> complexTypes) {
            this(namespace, containerName, typeMapping, idPropertyName, eis, complexTypes, true);
        }

        public InMemoryEdmGenerator(String namespace, String containerName, InMemoryTypeMapping typeMapping,
                String idPropertyName, Map<String, InfinispanProducer2.InMemoryEntityInfo<?>> eis,
                Map<String, InMemoryComplexTypeInfo<?>> complexTypes, boolean flatten) {
            this.namespace = namespace;
            this.containerName = containerName;
            this.typeMapping = typeMapping;
            this.eis = eis;
            this.complexTypeInfo = complexTypes;

            for (Map.Entry<String, InfinispanProducer2.InMemoryEntityInfo<?>> e : eis.entrySet()) {
                // e.getValue().entityClass = MyInternalCacheEntry , e.getKey() = "CacheEntries"
                // e.getValue() = InMemoryEntityInfo
                entitySetNameByClass.put(e.getValue().entityClass, e.getKey());
            }
            this.flatten = flatten;
        }

        @Override
        public EdmDataServices.Builder generateEdm(EdmDecorator decorator) {

            List<EdmSchema.Builder> schemas = new ArrayList<EdmSchema.Builder>();
            List<EdmEntityContainer.Builder> containers = new ArrayList<EdmEntityContainer.Builder>();
            List<EdmAssociation.Builder> associations = new ArrayList<EdmAssociation.Builder>();
            List<EdmAssociationSet.Builder> associationSets = new ArrayList<EdmAssociationSet.Builder>();



            createComplexTypes(decorator, edmComplexTypes);

            // creates id other basic SUPPORTED_TYPE properties(structural) entities
            createStructuralEntities(decorator);

            // TODO handle back references too
            // create hashmaps from sets

            // TODO!!!
            // Looks like I need this to properly register EntitySetNames!!!
            createNavigationProperties(associations, associationSets,
                    entityTypesByName, entitySetsByName, entitySetNameByClass);

            EdmEntityContainer.Builder container = EdmEntityContainer.newBuilder().
                    setName(containerName).setIsDefault(true).
                    addEntitySets(entitySetsByName.values()).addAssociationSets(associationSets);

            containers.add(container);

            EdmSchema.Builder schema = EdmSchema.newBuilder().setNamespace(namespace).
                    addEntityTypes(entityTypesByName.values()).addAssociations(associations).
                    addEntityContainers(containers).addComplexTypes(edmComplexTypes);

            addFunctions(schema, container);

            if (decorator != null) {
                schema.setDocumentation(decorator.getDocumentationForSchema(namespace));
                schema.setAnnotations(decorator.getAnnotationsForSchema(namespace));
            }

            schemas.add(schema);
            EdmDataServices.Builder rt = EdmDataServices.newBuilder().addSchemas(schemas);
            if (decorator != null) {
                rt.addNamespaces(decorator.getNamespaces());
            }
            return rt;
        }

        private void createComplexTypes(EdmDecorator decorator, List<EdmComplexType.Builder> complexTypes) {
            for (String complexTypeName : complexTypeInfo.keySet()) {
                dump("edm complexType: " + complexTypeName);
                InMemoryComplexTypeInfo<?> typeInfo = complexTypeInfo.get(complexTypeName);

                List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();

                // no keys
                properties.addAll(toEdmProperties(decorator, typeInfo.getPropertyModel(), new String[]{}, complexTypeName));

                EdmComplexType.Builder typeBuilder = EdmComplexType.newBuilder().setNamespace(namespace).setName(typeInfo.getTypeName()).addProperties(properties);

                if (decorator != null) {
                    typeBuilder.setDocumentation(decorator.getDocumentationForEntityType(namespace, complexTypeName));
                    typeBuilder.setAnnotations(decorator.getAnnotationsForEntityType(namespace, complexTypeName));
                }

                complexTypes.add(typeBuilder);
            }
        }

        private void createStructuralEntities(EdmDecorator decorator) {

            // eis contains all of the registered entity sets.
            for (String entitySetName : eis.keySet()) {
                InfinispanProducer2.InMemoryEntityInfo<?> entityInfo = eis.get(entitySetName);

                // do we have this type yet?
                EdmEntityType.Builder eet = entityTypesByName.get(entityInfo.entityTypeName);
                if (eet == null) {
                    eet = createStructuralType(decorator, entityInfo);
                }

                EdmEntitySet.Builder ees = EdmEntitySet.newBuilder().setName(entitySetName).setEntityType(eet);
                entitySetsByName.put(ees.getName(), ees);

            }
        }

        protected InfinispanProducer2.InMemoryEntityInfo<?> findEntityInfoForClass(Class<?> clazz) {
            for (InfinispanProducer2.InMemoryEntityInfo<?> typeInfo : this.eis.values()) {
                if (typeInfo.entityClass.equals(clazz)) {
                    return typeInfo;
                }
            }

            return null;
        }

        /*
         * contains all generated InMemoryEntityInfos that get created as we walk
         * up the inheritance hierarchy and find Java types that are not registered.
         */
        private Map<Class<?>, InfinispanProducer2.InMemoryEntityInfo<?>> unregisteredEntityInfo =
                new HashMap<Class<?>, InfinispanProducer2.InMemoryEntityInfo<?>>();

        protected InfinispanProducer2.InMemoryEntityInfo<?> getUnregisteredEntityInfo(Class<?> clazz, InfinispanProducer2.InMemoryEntityInfo<?> subclass) {
            InfinispanProducer2.InMemoryEntityInfo<?> ei = unregisteredEntityInfo.get(clazz);
            if (ei == null) {
                ei = new InfinispanProducer2.InMemoryEntityInfo();
                ei.entityTypeName = clazz.getSimpleName();
                ei.keys = subclass.keys;
                ei.entityClass = (Class) clazz;
                ei.properties = new EnumsAsStringsPropertyModelDelegate(
                        new BeanBasedPropertyModel(ei.entityClass, this.flatten));
            }
            return ei;
        }

        protected EdmEntityType.Builder createStructuralType(EdmDecorator decorator, InfinispanProducer2.InMemoryEntityInfo<?> entityInfo) {
            List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();

            Class<?> superClass = flatten ? null : entityInfo.getSuperClass();

            properties.addAll(toEdmProperties(decorator, entityInfo.properties, entityInfo.keys, entityInfo.entityTypeName));

            EdmEntityType.Builder eet = EdmEntityType.newBuilder().setNamespace(namespace).setName(entityInfo.entityTypeName).setHasStream(entityInfo.hasStream).addProperties(properties);

            if (superClass == null) {
                eet.addKeys(entityInfo.keys);
            }

            if (decorator != null) {
                eet.setDocumentation(decorator.getDocumentationForEntityType(namespace, entityInfo.entityTypeName));
                eet.setAnnotations(decorator.getAnnotationsForEntityType(namespace, entityInfo.entityTypeName));
            }
            entityTypesByName.put(eet.getName(), eet);

            EdmEntityType.Builder superType = null;
            if (!this.flatten && entityInfo.entityClass.getSuperclass() != null && !entityInfo.entityClass.getSuperclass().equals(Object.class)) {
                InfinispanProducer2.InMemoryEntityInfo<?> entityInfoSuper = findEntityInfoForClass(entityInfo.entityClass.getSuperclass());
                // may have created it along another branch in the hierarchy
                if (entityInfoSuper == null) {
                    // synthesize...
                    entityInfoSuper = getUnregisteredEntityInfo(entityInfo.entityClass.getSuperclass(), entityInfo);
                }

                superType = entityTypesByName.get(entityInfoSuper.entityTypeName);
                if (superType == null) {
                    superType = createStructuralType(decorator, entityInfoSuper);
                }
            }

            eet.setBaseType(superType);
            return eet;
        }

        protected void createNavigationProperties(List<EdmAssociation.Builder> associations,
                List<EdmAssociationSet.Builder> associationSets,
                Map<String, EdmEntityType.Builder> entityTypesByName,
                Map<String, EdmEntitySet.Builder> entitySetByName,
                Map<Class<?>, String> entityNameByClass) {

            for (String entitySetName : eis.keySet()) {
                InfinispanProducer2.InMemoryEntityInfo<?> ei = eis.get(entitySetName);
                Class<?> clazz1 = ei.entityClass;

                generateToOneNavProperties(associations, associationSets,
                        entityTypesByName, entitySetByName, entityNameByClass,
                        ei.entityTypeName, ei);

                generateToManyNavProperties(associations, associationSets,
                        entityTypesByName, entitySetByName, entityNameByClass,
                        ei.entityTypeName, ei, clazz1);
            }
        }

        protected void generateToOneNavProperties(
                List<EdmAssociation.Builder> associations,
                List<EdmAssociationSet.Builder> associationSets,
                Map<String, EdmEntityType.Builder> entityTypesByName,
                Map<String, EdmEntitySet.Builder> entitySetByName,
                Map<Class<?>, String> entityNameByClass,
                String entityTypeName,
                InfinispanProducer2.InMemoryEntityInfo<?> ei) {

            Iterable<String> propertyNames = this.flatten ? ei.properties.getPropertyNames() : ei.properties.getDeclaredPropertyNames();
            for (String assocProp : propertyNames) {

                EdmEntityType.Builder eet1 = entityTypesByName.get(entityTypeName);
                Class<?> clazz2 = ei.properties.getPropertyType(assocProp);
                String entitySetName2 = entityNameByClass.get(clazz2);
                InfinispanProducer2.InMemoryEntityInfo<?> ei2 = entitySetName2 == null ? null : eis.get(entitySetName2);

                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, "genToOnNavProp {0} - {1}({2}) eetName2: {3}", new Object[]{entityTypeName, assocProp, clazz2, entitySetName2});
                }

                if (eet1.findProperty(assocProp) != null || ei2 == null) {
                    continue;
                }

                EdmEntityType.Builder eet2 = entityTypesByName.get(ei2.entityTypeName);

                EdmMultiplicity m1 = EdmMultiplicity.MANY;
                EdmMultiplicity m2 = EdmMultiplicity.ONE;

                String assocName = String.format("FK_%s_%s", eet1.getName(), eet2.getName());
                EdmAssociationEnd.Builder assocEnd1 = EdmAssociationEnd.newBuilder().setRole(eet1.getName()).setType(eet1).setMultiplicity(m1);
                String assocEnd2Name = eet2.getName();
                if (assocEnd2Name.equals(eet1.getName())) {
                    assocEnd2Name = assocEnd2Name + "1";
                }

                EdmAssociationEnd.Builder assocEnd2 = EdmAssociationEnd.newBuilder().setRole(assocEnd2Name).setType(eet2).setMultiplicity(m2);
                EdmAssociation.Builder assoc = EdmAssociation.newBuilder().setNamespace(namespace).setName(assocName).setEnds(assocEnd1, assocEnd2);

                associations.add(assoc);

                EdmEntitySet.Builder ees1 = entitySetByName.get(eet1.getName());
                EdmEntitySet.Builder ees2 = entitySetByName.get(eet2.getName());
                if (ees1 == null) {
                    // entity set name different than entity type name.
                    ees1 = getEntitySetForEntityTypeName(eet1.getName());
                }
                if (ees2 == null) {
                    // entity set name different than entity type name.
                    ees2 = getEntitySetForEntityTypeName(eet2.getName());
                }

                EdmAssociationSet.Builder eas = EdmAssociationSet.newBuilder().setName(assocName).setAssociation(assoc).setEnds(
                        EdmAssociationSetEnd.newBuilder().setRole(assocEnd1).setEntitySet(ees1),
                        EdmAssociationSetEnd.newBuilder().setRole(assocEnd2).setEntitySet(ees2));

                associationSets.add(eas);

                EdmNavigationProperty.Builder np = EdmNavigationProperty.newBuilder(assocProp).setRelationship(assoc).setFromTo(assoc.getEnd1(), assoc.getEnd2());

                eet1.addNavigationProperties(np);
            }
        }

        protected EdmEntitySet.Builder getEntitySetForEntityTypeName(String entityTypeName) {

            for (InfinispanProducer2.InMemoryEntityInfo<?> ti : eis.values()) {
                if (ti.entityTypeName.equals(entityTypeName)) {
                    return entitySetsByName.get(ti.entitySetName);
                }
            }
            return null;
        }

        protected void generateToManyNavProperties(List<EdmAssociation.Builder> associations,
                List<EdmAssociationSet.Builder> associationSets,
                Map<String, EdmEntityType.Builder> entityTypesByName,
                Map<String, EdmEntitySet.Builder> entitySetByName,
                Map<Class<?>, String> entityNameByClass,
                String entityTypeName,
                InfinispanProducer2.InMemoryEntityInfo<?> ei,
                Class<?> clazz1) {

            Iterable<String> collectionNames = this.flatten ? ei.properties.getCollectionNames() : ei.properties.getDeclaredCollectionNames();

            for (String assocProp : collectionNames) {

                final EdmEntityType.Builder eet1 = entityTypesByName.get(entityTypeName);

                Class<?> clazz2 = ei.properties.getCollectionElementType(assocProp);
                String entitySetName2 = entityNameByClass.get(clazz2);
                InfinispanProducer2.InMemoryEntityInfo<?> class2eiInfo = entitySetName2 == null ? null : eis.get(entitySetName2);

                if (class2eiInfo == null) {
                    continue;
                }

                final EdmEntityType.Builder eet2 = entityTypesByName.get(class2eiInfo.entityTypeName);

                try {
                    EdmAssociation.Builder assoc = Enumerable.create(associations).firstOrNull(new Predicate1<EdmAssociation.Builder>() {

                        public boolean apply(EdmAssociation.Builder input) {
                            return input.getEnd1().getType().equals(eet2) && input.getEnd2().getType().equals(eet1);
                        }
                    });

                    EdmAssociationEnd.Builder fromRole, toRole;

                    if (assoc == null) {
                        //no association already exists
                        EdmMultiplicity m1 = EdmMultiplicity.ZERO_TO_ONE;
                        EdmMultiplicity m2 = EdmMultiplicity.MANY;

                        //find ei info of class2
                        for (String tmp : class2eiInfo.properties.getCollectionNames()) {
                            //class2 has a ref to class1
                            //Class<?> tmpc = class2eiInfo.properties.getCollectionElementType(tmp);
                            if (clazz1 == class2eiInfo.properties.getCollectionElementType(tmp)) {
                                m1 = EdmMultiplicity.MANY;
                                m2 = EdmMultiplicity.MANY;
                                break;
                            }
                        }

                        String assocName = String.format("FK_%s_%s", eet1.getName(), eet2.getName());
                        EdmAssociationEnd.Builder assocEnd1 = EdmAssociationEnd.newBuilder().setRole(eet1.getName()).setType(eet1).setMultiplicity(m1);
                        String assocEnd2Name = eet2.getName();
                        if (assocEnd2Name.equals(eet1.getName())) {
                            assocEnd2Name = assocEnd2Name + "1";
                        }
                        EdmAssociationEnd.Builder assocEnd2 = EdmAssociationEnd.newBuilder().setRole(assocEnd2Name).setType(eet2).setMultiplicity(m2);
                        assoc = EdmAssociation.newBuilder().setNamespace(namespace).setName(assocName).setEnds(assocEnd1, assocEnd2);

                        associations.add(assoc);

                        EdmEntitySet.Builder ees1 = entitySetByName.get(eet1.getName());
                        EdmEntitySet.Builder ees2 = entitySetByName.get(eet2.getName());
                        if (ees1 == null) {
                            // entity set name different than entity type name.
                            ees1 = getEntitySetForEntityTypeName(eet1.getName());
                        }
                        if (ees2 == null) {
                            // entity set name different than entity type name.
                            ees2 = getEntitySetForEntityTypeName(eet2.getName());
                        }

                        EdmAssociationSet.Builder eas = EdmAssociationSet.newBuilder().setName(assocName).setAssociation(assoc).setEnds(
                                EdmAssociationSetEnd.newBuilder().setRole(assocEnd1).setEntitySet(ees1),
                                EdmAssociationSetEnd.newBuilder().setRole(assocEnd2).setEntitySet(ees2));
                        associationSets.add(eas);

                        fromRole = assoc.getEnd1();
                        toRole = assoc.getEnd2();
                    } else {
                        fromRole = assoc.getEnd2();
                        toRole = assoc.getEnd1();
                    }

                    EdmNavigationProperty.Builder np = EdmNavigationProperty.newBuilder(assocProp).setRelationship(assoc).setFromTo(fromRole, toRole);

                    eet1.addNavigationProperties(np);
                } catch (Exception e) {
                    // hmmh...I guess the build() will fail later
                    log.log(Level.WARNING, "Exception building Edm associations: " + entityTypeName + "," + clazz1 + " set: " + ei.entitySetName
                            + " -> " + assocProp, e);
                }
            }
        }

        protected EdmComplexType.Builder findComplexTypeBuilder(String typeName) {
            String fqName = this.namespace + "." + typeName;
            for (EdmComplexType.Builder builder : this.edmComplexTypes) {
                if (builder.getFullyQualifiedTypeName().equals(fqName)) {
                    return builder;
                }
            }
            return null;
        }

        protected EdmComplexType.Builder findComplexTypeForClass(Class<?> clazz) {
            for (InMemoryComplexTypeInfo<?> typeInfo : this.complexTypeInfo.values()) {
                if (typeInfo.getEntityClass().equals(clazz)) {
                    // the typeName defines the edm type name
                    return findComplexTypeBuilder(typeInfo.getTypeName());
                }
            }

            return null;
        }

        private Collection<EdmProperty.Builder> toEdmProperties(
                EdmDecorator decorator,
                PropertyModel model,
                String[] keys,
                String structuralTypename) {

            List<EdmProperty.Builder> rt = new ArrayList<EdmProperty.Builder>();
            Set<String> keySet = Enumerable.create(keys).toSet();

            Iterable<String> propertyNames = this.flatten ? model.getPropertyNames() : model.getDeclaredPropertyNames();
            for (String propName : propertyNames) {
                dump("edm property: " + propName);
                Class<?> propType = model.getPropertyType(propName);
                EdmType type = typeMapping.findEdmType(propType);
                EdmComplexType.Builder typeBuilder = null;
                if (type == null) {
                    typeBuilder = findComplexTypeForClass(propType);
                }

                dump("edm property: " + propName + " type: " + type + " builder: " + typeBuilder);
                if (type == null && typeBuilder == null) {
                    continue;
                }

                EdmProperty.Builder ep = EdmProperty.newBuilder(propName).setNullable(!keySet.contains(propName));

                if (type != null) {
                    ep.setType(type);
                } else {
                    ep.setType(typeBuilder);
                }

                if (decorator != null) {
                    ep.setDocumentation(decorator.getDocumentationForProperty(namespace, structuralTypename, propName));
                    ep.setAnnotations(decorator.getAnnotationsForProperty(namespace, structuralTypename, propName));
                }
                rt.add(ep);
            }

            // collections of primitives and complex types
            propertyNames = this.flatten ? model.getCollectionNames() : model.getDeclaredCollectionNames();
            for (String collectionPropName : propertyNames) {
                Class<?> collectionElementType = model.getCollectionElementType(collectionPropName);
                if (entitySetNameByClass.get(collectionElementType) != null) {
                    // this will be a nav prop
                    continue;
                }

                EdmType type = typeMapping.findEdmType(collectionElementType);
                EdmType.Builder typeBuilder = null;
                if (type == null) {
                    typeBuilder = findComplexTypeForClass(collectionElementType);
                } else {
                    typeBuilder = EdmSimpleType.newBuilder(type);
                }

                if (typeBuilder == null) {
                    continue;
                }

                // either a simple or complex type.
                EdmProperty.Builder ep = EdmProperty.newBuilder(collectionPropName).setNullable(true).setCollectionKind(EdmProperty.CollectionKind.Collection).setType(typeBuilder);

                if (decorator != null) {
                    ep.setDocumentation(decorator.getDocumentationForProperty(namespace, structuralTypename, collectionPropName));
                    ep.setAnnotations(decorator.getAnnotationsForProperty(namespace, structuralTypename, collectionPropName));
                }
                rt.add(ep);
            }

            return rt;
        }

        /**
         * get the Edm namespace
         *
         * @return the Edm namespace
         */
        public String getNamespace() {
            return namespace;
        }

        /**
         * provides an override point for applications to add application specific EdmFunctions to their producer.
         *
         * @param schema    the EdmSchema.Builder
         * @param container the EdmEntityContainer.Builder
         */
        protected void addFunctions(EdmSchema.Builder schema, EdmEntityContainer.Builder container) {
            // overridable :)
        }
    }
}
