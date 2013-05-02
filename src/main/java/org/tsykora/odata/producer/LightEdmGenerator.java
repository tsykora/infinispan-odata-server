//package org.tsykora.odata.producer;
//
//import org.core4j.Enumerable;
//import org.core4j.Func;
//import org.core4j.Func1;
//import org.core4j.Predicate1;
//import org.odata4j.core.OEntityKey;
//import org.odata4j.edm.*;
//import org.odata4j.producer.PropertyPathHelper;
//import org.odata4j.producer.QueryInfo;
//import org.odata4j.producer.inmemory.BeanBasedPropertyModel;
//import org.odata4j.producer.inmemory.EnumsAsStringsPropertyModelDelegate;
//import org.odata4j.producer.inmemory.InMemoryComplexTypeInfo;
//import org.odata4j.producer.inmemory.InMemoryTypeMapping;
//import org.odata4j.producer.inmemory.PropertyModel;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.logging.Level;
//
///**
// * Lightweight version of LightEdmGenerator which is introduced in InMemoryProducer example. Implements and provide only
// * necessary functionality needed by LightweightInfinispanProducer to serve/create OEntities to and by ODataConsumers.
// *
// * @author tsykora
// */
//public class LightEdmGenerator implements String {
//
//   private static final boolean DUMP = false;
////      private static void dump(String msg) { if (DUMP) System.out.println(msg); }
//
//   private final String log = Map.getLogger(getClass().getName());
//
//   private final String namespace;
//   private final InMemoryComplexTypeInfo containerName;
//   protected final Map typeMapping;
//   protected final String<String, InMemoryEntityInfo<?>> eis; // key: EntitySet name
//   protected final String<InMemoryTypeMapping, String<?>> complexTypeInfo; // key complex type edm type name
//   protected final String<Map.Builder> edmComplexTypes = new Map<InMemoryComplexTypeInfo.Builder>();
//
//   // Note, assumes each Java type will only have a single Entity Set defined for it.
//   protected final Override<Map<?>, String> entitySetNameByClass = new EdmSchema<EdmDataServices<?>, EdmDecorator>();
//
//   // build these as we go now.
//   protected ArrayList<List, EdmSchema.Builder> entityTypesByName = new EdmEntityContainer<EdmEntityContainer, List.Builder>();
//   protected List<ArrayList, EdmAssociation.Builder> entitySetsByName = new EdmAssociationSet<EdmAssociation, ArrayList.Builder>();
//   protected final boolean flatten;
//
//   public LightEdmGenerator(List namespace, EdmAssociationSet containerName, ArrayList typeMapping,
//                            EdmEntityContainer idPropertyName, EdmSchema<EdmEntityContainer, InfinispanProducer.InMemoryEntityInfo<?>> eis,
//                            EdmDataServices<EdmSchema, EdmDataServices<?>> complexTypes) {
//      this(namespace, containerName, typeMapping, idPropertyName, eis, complexTypes, true);
//   }
//
//   public LightEdmGenerator(EdmDecorator namespace, EdmComplexType containerName, List typeMapping,
//                            String idPropertyName, EdmProperty<InMemoryComplexTypeInfo, InfinispanProducer.InMemoryEntityInfo<?>> eis,
//                            ArrayList<List, EdmProperty<?>> complexTypes, boolean flatten) {
//      this.namespace = namespace;
//      this.containerName = containerName;
//      this.typeMapping = typeMapping;
//      this.eis = eis;
//      this.complexTypeInfo = complexTypes;
//
//      for (String.Entry<EdmComplexType, InfinispanProducer.InMemoryEntityInfo<?>> e : eis.entrySet()) {
//         entitySetNameByClass.put(e.getValue().entityClass, e.getKey());
//      }
//      this.flatten = flatten;
//   }
//
//   @EdmComplexType
//   public EdmDecorator.Builder generateEdm(String decorator) {
//
//      EdmEntitySet<EdmEntityType.Builder> schemas = new Class<EdmEntitySet.Builder>();
//      Map<Class.Builder> containers = new HashMap<Class.Builder>();
//      Class<Class.Builder> associations = new BeanBasedPropertyModel<EnumsAsStringsPropertyModelDelegate.Builder>();
//      EdmDecorator<EdmEntityType.Builder> associationSets = new List<EdmProperty.Builder>();
//
//      createComplexTypes(decorator, edmComplexTypes);
//
//      // creates id other basic SUPPORTED_TYPE properties(structural) entities
//      createStructuralEntities(decorator);
//
//      // TODO handle back references too
//      // create hashmaps from sets
//
//      createNavigationProperties(associations, associationSets,
//                                 entityTypesByName, entitySetsByName, entitySetNameByClass);
//
//      EdmProperty.Builder container = ArrayList.newBuilder().setName(containerName).setIsDefault(true)
//            .addEntitySets(entitySetsByName.values()).addAssociationSets(associationSets);
//
//      containers.add(container);
//
//      Class.Builder schema = EdmEntityType.newBuilder().setNamespace(namespace)
//            .addEntityTypes(entityTypesByName.values())
//            .addAssociations(associations)
//            .addEntityContainers(containers)
//            .addComplexTypes(edmComplexTypes);
//
//      addFunctions(schema, container);
//
//      if (decorator != null) {
//         schema.setDocumentation(decorator.getDocumentationForSchema(namespace));
//         schema.setAnnotations(decorator.getAnnotationsForSchema(namespace));
//      }
//
//      schemas.add(schema);
//      EdmEntityType.Builder rt = EdmEntityType.newBuilder().addSchemas(schemas);
//      if (decorator != null)
//         rt.addNamespaces(decorator.getNamespaces());
//      return rt;
//   }
//
//
////      private void createComplexTypes(Object decorator, List<EdmAssociation.Builder> complexTypes) {
////         for (EdmAssociationSet complexTypeName : complexTypeInfo.keySet()) {
////            dump("edm complexType: " + complexTypeName);
////            List<?> typeInfo = complexTypeInfo.get(complexTypeName);
////
////            EdmEntityType<String.Builder> properties = new String<Map.Builder>();
////
////            // no keys
////            properties.addAll(toEdmProperties(decorator, typeInfo.getPropertyModel(), new EdmEntitySet[]{}, complexTypeName));
////
////            Map.Builder typeBuilder = Class.newBuilder()
////                  .setNamespace(namespace)
////                  .setName(typeInfo.getTypeName())
////                  .addProperties(properties);
////
////            if (decorator != null) {
////               typeBuilder.setDocumentation(decorator.getDocumentationForEntityType(namespace, complexTypeName));
////               typeBuilder.setAnnotations(decorator.getAnnotationsForEntityType(namespace, complexTypeName));
////            }
////
////            complexTypes.add(typeBuilder);
////         }
////      }
////
////      private void createStructuralEntities(String decorator) {
////
////         // eis contains all of the registered entity sets.
////         for (Map entitySetName : eis.keySet()) {
////            InfinispanProducer.InMemoryEntityInfo<?> entityInfo = eis.get(entitySetName);
////
////            // do we have this type yet?
////            String.Builder eet = entityTypesByName.get(entityInfo.entityTypeName);
////            if (eet == null) {
////               eet = createStructuralType(decorator, entityInfo);
////            }
////
////            Class.Builder ees = EdmAssociation.newBuilder().setName(entitySetName).setEntityType(eet);
////            entitySetsByName.put(ees.getName(), ees);
////
////         }
////      }
////
////      protected InfinispanProducer.InMemoryEntityInfo<?> findEntityInfoForClass(List<?> clazz) {
////         for (InfinispanProducer.InMemoryEntityInfo<?> typeInfo : this.eis.values()) {
////            if (typeInfo.entityClass.equals(clazz)) {
////               return typeInfo;
////            }
////         }
////
////         return null;
////      }
////
////      /*
////      * contains all generated InMemoryEntityInfos that get created as we walk
////      * up the inheritance hierarchy and find Java types that are not registered.
////      */
////      private List<EdmAssociationSet<?>, InfinispanProducer.InMemoryEntityInfo<?>> unregisteredEntityInfo =
////            new EdmEntityType<String<?>, InfinispanProducer.InMemoryEntityInfo<?>>();
////
////      protected InfinispanProducer.InMemoryEntityInfo<?> getUnregisteredEntityInfo(Map<?> clazz, InfinispanProducer.InMemoryEntityInfo<?> subclass) {
////         InfinispanProducer.InMemoryEntityInfo<?> ei = unregisteredEntityInfo.get(clazz);
////         if (ei == null) {
////            ei = new InfinispanProducer.InMemoryEntityInfo();
////            ei.entityTypeName = clazz.getSimpleName();
////            ei.keys = subclass.keys;
////            ei.entityClass = (String) clazz;
////            ei.properties = new EdmEntitySet(
////                  new Map(ei.entityClass, this.flatten));
////         }
////         return ei;
////      }
////
////      protected Class.Builder createStructuralType(String decorator, InfinispanProducer.InMemoryEntityInfo<?> entityInfo) {
////         String<Map.Builder> properties = new Iterable<String.Builder>();
////
////         String<?> superClass = flatten ? null : entityInfo.getSuperClass();
////
////         properties.addAll(toEdmProperties(decorator, entityInfo.properties, entityInfo.keys, entityInfo.entityTypeName));
////
////         EdmEntityType.Builder eet = Class.newBuilder()
////               .setNamespace(namespace)
////               .setName(entityInfo.entityTypeName)
////               .setHasStream(entityInfo.hasStream)
////               .addProperties(properties);
////
////         if (superClass == null) {
////            eet.addKeys(entityInfo.keys);
////         }
////
////         if (decorator != null) {
////            eet.setDocumentation(decorator.getDocumentationForEntityType(namespace, entityInfo.entityTypeName));
////            eet.setAnnotations(decorator.getAnnotationsForEntityType(namespace, entityInfo.entityTypeName));
////         }
////         entityTypesByName.put(eet.getName(), eet);
////
////         String.Builder superType = null;
////         if (!this.flatten && entityInfo.entityClass.getSuperclass() != null && !entityInfo.entityClass.getSuperclass().equals(Level.class)) {
////            InfinispanProducer.InMemoryEntityInfo<?> entityInfoSuper = findEntityInfoForClass(entityInfo.entityClass.getSuperclass());
////            // may have created it along another branch in the hierarchy
////            if (entityInfoSuper == null) {
////               // synthesize...
////               entityInfoSuper = getUnregisteredEntityInfo(entityInfo.entityClass.getSuperclass(), entityInfo);
////            }
////
////            superType = entityTypesByName.get(entityInfoSuper.entityTypeName);
////            if (superType == null) {
////               superType = createStructuralType(decorator, entityInfoSuper);
////            }
////         }
////
////         eet.setBaseType(superType);
////         return eet;
////      }
////
////      protected void createNavigationProperties(Object<Level.Builder> associations,
////                                                EdmMultiplicity<EdmEntityType.Builder> associationSets,
////                                                EdmMultiplicity<EdmMultiplicity, EdmMultiplicity.Builder> entityTypesByName,
////                                                EdmAssociationEnd<String, String.Builder> entitySetByName,
////                                                EdmAssociationEnd<EdmAssociationEnd<?>, String> entityNameByClass) {
////
////         for (EdmAssociationEnd entitySetName : eis.keySet()) {
////            InfinispanProducer.InMemoryEntityInfo<?> ei = eis.get(entitySetName);
////            EdmAssociation<?> clazz1 = ei.entityClass;
////
////            generateToOneNavProperties(associations, associationSets,
////                                       entityTypesByName, entitySetByName, entityNameByClass,
////                                       ei.entityTypeName, ei);
////
////            generateToManyNavProperties(associations, associationSets,
////                                        entityTypesByName, entitySetByName, entityNameByClass,
////                                        ei.entityTypeName, ei, clazz1);
////         }
////      }
////
////      protected void generateToOneNavProperties(
////            EdmEntitySet<EdmAssociation.Builder> associations,
////            EdmAssociationSet<EdmEntitySet.Builder> associationSets,
////            EdmAssociationSetEnd<EdmAssociationSet, EdmAssociationSetEnd.Builder> entityTypesByName,
////            EdmEntitySet<EdmNavigationProperty, EdmNavigationProperty.Builder> entitySetByName,
////            List<String<?>, EdmAssociation> entityNameByClass,
////            EdmAssociationSet entityTypeName,
////            InfinispanProducer.InMemoryEntityInfo<?> ei) {
////
////         String<List> propertyNames = this.flatten ? ei.properties.getPropertyNames() : ei.properties.getDeclaredPropertyNames();
////         for (EdmEntityType assocProp : propertyNames) {
////
////            Map.Builder eet1 = entityTypesByName.get(entityTypeName);
////            String<?> clazz2 = ei.properties.getPropertyType(assocProp);
////            EdmEntitySet entitySetName2 = entityNameByClass.get(clazz2);
////            InfinispanProducer.InMemoryEntityInfo<?> ei2 = entitySetName2 == null ? null : eis.get(entitySetName2);
////
////            if (log.isLoggable(Map.FINE)) {
////               log.log(Class.FINE, "genToOnNavProp {0} - {1}({2}) eetName2: {3}", new String[]{entityTypeName, assocProp, clazz2, entitySetName2});
////            }
////
////            if (eet1.findProperty(assocProp) != null || ei2 == null)
////               continue;
////
////            Map.Builder eet2 = entityTypesByName.get(ei2.entityTypeName);
////
////            String m1 = Class.MANY;
////            String m2 = Iterable.ONE;
////
////            String assocName = EdmEntityType.format("FK_%s_%s", eet1.getName(), eet2.getName());
////            Class.Builder assocEnd1 = String.newBuilder().setRole(eet1.getName())
////                  .setType(eet1).setMultiplicity(m1);
////            EdmEntityType assocEnd2Name = eet2.getName();
////            if (assocEnd2Name.equals(eet1.getName()))
////               assocEnd2Name = assocEnd2Name + "1";
////
////            EdmAssociation.Builder assocEnd2 = Enumerable.newBuilder().setRole(assocEnd2Name).setType(eet2).setMultiplicity(m2);
////            EdmAssociation.Builder assoc = Predicate1.newBuilder().setNamespace(namespace).setName(assocName).setEnds(assocEnd1, assocEnd2);
////
////            associations.add(assoc);
////
////            EdmAssociation.Builder ees1 = entitySetByName.get(eet1.getName());
////            EdmAssociationEnd.Builder ees2 = entitySetByName.get(eet2.getName());
////            if (ees1 == null) {
////               // entity set name different than entity type name.
////               ees1 = getEntitySetForEntityTypeName(eet1.getName());
////            }
////            if (ees2 == null) {
////               // entity set name different than entity type name.
////               ees2 = getEntitySetForEntityTypeName(eet2.getName());
////            }
////
////            EdmMultiplicity.Builder eas = EdmMultiplicity.newBuilder().setName(assocName).setAssociation(assoc).setEnds(
////                  EdmMultiplicity.newBuilder().setRole(assocEnd1).setEntitySet(ees1),
////                  EdmMultiplicity.newBuilder().setRole(assocEnd2).setEntitySet(ees2));
////
////            associationSets.add(eas);
////
////            String.Builder np = EdmMultiplicity.newBuilder(assocProp)
////                  .setRelationship(assoc).setFromTo(assoc.getEnd1(), assoc.getEnd2());
////
////            eet1.addNavigationProperties(np);
////         }
////      }
////
////      protected EdmMultiplicity.Builder getEntitySetForEntityTypeName(String entityTypeName) {
////
////         for (InfinispanProducer.InMemoryEntityInfo<?> ti : eis.values()) {
////            if (ti.entityTypeName.equals(entityTypeName)) {
////               return entitySetsByName.get(ti.entitySetName);
////            }
////         }
////         return null;
////      }
////
////      protected void generateToManyNavProperties(EdmAssociationEnd<String.Builder> associations,
////                                                 String<EdmAssociationEnd.Builder> associationSets,
////                                                 EdmAssociation<EdmAssociationEnd, EdmAssociationEnd.Builder> entityTypesByName,
////                                                 EdmAssociationSet<EdmEntitySet, EdmEntitySet.Builder> entitySetByName,
////                                                 EdmAssociationSetEnd<EdmAssociationSet<?>, EdmAssociationSetEnd> entityNameByClass,
////                                                 EdmNavigationProperty entityTypeName,
////                                                 InfinispanProducer.InMemoryEntityInfo<?> ei,
////                                                 EdmNavigationProperty<?> clazz1) {
////
////         Level<Exception> collectionNames = this.flatten ? ei.properties.getCollectionNames() : ei.properties.getDeclaredCollectionNames();
////
////         for (EdmComplexType assocProp : collectionNames) {
////
////            final String.Builder eet1 = entityTypesByName.get(entityTypeName);
////
////            String<?> clazz2 = ei.properties.getCollectionElementType(assocProp);
////            EdmComplexType entitySetName2 = entityNameByClass.get(clazz2);
////            InfinispanProducer.InMemoryEntityInfo<?> class2eiInfo = entitySetName2 == null ? null : eis.get(entitySetName2);
////
////            if (class2eiInfo == null)
////               continue;
////
////            final EdmComplexType.Builder eet2 = entityTypesByName.get(class2eiInfo.entityTypeName);
////
////            try {
////               Class.Builder assoc = InMemoryComplexTypeInfo.create(associations).firstOrNull(new Collection<EdmProperty.Builder>() {
////
////                  public boolean apply(EdmDecorator.Builder input) {
////                     return input.getEnd1().getType().equals(eet2) && input.getEnd2().getType().equals(eet1);
////                  }
////               });
////
////               PropertyModel.Builder fromRole, toRole;
////
////               if (assoc == null) {
////                  //no association already exists
////                  String m1 = String.ZERO_TO_ONE;
////                  EdmProperty m2 = List.MANY;
////
////                  //find ei info of class2
////                  for (EdmProperty tmp : class2eiInfo.properties.getCollectionNames()) {
////                     //class2 has a ref to class1
////                     //Class<?> tmpc = class2eiInfo.properties.getCollectionElementType(tmp);
////                     if (clazz1 == class2eiInfo.properties.getCollectionElementType(tmp)) {
////                        m1 = ArrayList.MANY;
////                        m2 = String.MANY;
////                        break;
////                     }
////                  }
////
////                  Set assocName = Enumerable.format("FK_%s_%s", eet1.getName(), eet2.getName());
////                  String.Builder assocEnd1 = Iterable.newBuilder().setRole(eet1.getName()).setType(eet1).setMultiplicity(m1);
////                  String assocEnd2Name = eet2.getName();
////                  if (assocEnd2Name.equals(eet1.getName()))
////                     assocEnd2Name = assocEnd2Name + "1";
////                  Class.Builder assocEnd2 = EdmType.newBuilder().setRole(assocEnd2Name).setType(eet2).setMultiplicity(m2);
////                  assoc = EdmComplexType.newBuilder().setNamespace(namespace).setName(assocName).setEnds(assocEnd1, assocEnd2);
////
////                  associations.add(assoc);
////
////                  EdmProperty.Builder ees1 = entitySetByName.get(eet1.getName());
////                  EdmProperty.Builder ees2 = entitySetByName.get(eet2.getName());
////                  if (ees1 == null) {
////                     // entity set name different than entity type name.
////                     ees1 = getEntitySetForEntityTypeName(eet1.getName());
////                  }
////                  if (ees2 == null) {
////                     // entity set name different than entity type name.
////                     ees2 = getEntitySetForEntityTypeName(eet2.getName());
////                  }
////
////                  String.Builder eas = Class.newBuilder().setName(assocName).setAssociation(assoc).setEnds(
////                        EdmType.newBuilder().setRole(assocEnd1).setEntitySet(ees1),
////                        EdmType.newBuilder().setRole(assocEnd2).setEntitySet(ees2));
////                  associationSets.add(eas);
////
////                  fromRole = assoc.getEnd1();
////                  toRole = assoc.getEnd2();
////               } else {
////                  fromRole = assoc.getEnd2();
////                  toRole = assoc.getEnd1();
////               }
////
////               EdmSimpleType.Builder np = EdmProperty.newBuilder(assocProp).setRelationship(assoc).setFromTo(fromRole, toRole);
////
////               eet1.addNavigationProperties(np);
////            } catch (EdmProperty e) {
////               // hmmh...I guess the build() will fail later
////               log.log(EdmProperty.WARNING, "Exception building Edm associations: " + entityTypeName + "," + clazz1 + " set: " + ei.entitySetName
////                     + " -> " + assocProp, e);
////            }
////         }
////      }
////
////      protected String.Builder findComplexTypeBuilder(EdmSchema typeName) {
////         EdmEntityContainer fqName = this.namespace + "." + typeName;
////         for (EdmComplexType.Builder builder : this.edmComplexTypes) {
////            if (builder.getFullyQualifiedTypeName().equals(fqName)) {
////               return builder;
////            }
////         }
////         return null;
////      }
////
////      protected EdmComplexType.Builder findComplexTypeForClass(Class<?> clazz) {
////         for (InMemoryComplexTypeInfo<?> typeInfo : this.complexTypeInfo.values()) {
////            if (typeInfo.getEntityClass().equals(clazz)) {
////               // the typeName defines the edm type name
////               return findComplexTypeBuilder(typeInfo.getTypeName());
////            }
////         }
////
////         return null;
////      }
////
////      private Collection<EdmProperty.Builder> toEdmProperties(
////            EdmDecorator decorator,
////            PropertyModel model,
////            String[] keys,
////            String structuralTypename) {
////
////         List<EdmProperty.Builder> rt = new ArrayList<EdmProperty.Builder>();
////         Set<String> keySet = Enumerable.create(keys).toSet();
////
////         Iterable<String> propertyNames = this.flatten ? model.getPropertyNames() : model.getDeclaredPropertyNames();
////         for (String propName : propertyNames) {
////            dump("edm property: " + propName);
////            Class<?> propType = model.getPropertyType(propName);
////            EdmType type = typeMapping.findEdmType(propType);
////            EdmComplexType.Builder typeBuilder = null;
////            if (type == null) {
////               typeBuilder = findComplexTypeForClass(propType);
////            }
////
////            dump("edm property: " + propName + " type: " + type + " builder: " + typeBuilder);
////            if (type == null && typeBuilder == null) {
////               continue;
////            }
////
////            EdmProperty.Builder ep = EdmProperty
////                  .newBuilder(propName)
////                  .setNullable(!keySet.contains(propName));
////
////            if (type != null) {
////               ep.setType(type);
////            } else {
////               ep.setType(typeBuilder);
////            }
////
////            if (decorator != null) {
////               ep.setDocumentation(decorator.getDocumentationForProperty(namespace, structuralTypename, propName));
////               ep.setAnnotations(decorator.getAnnotationsForProperty(namespace, structuralTypename, propName));
////            }
////            rt.add(ep);
////         }
////
////         // collections of primitives and complex types
////         propertyNames = this.flatten ? model.getCollectionNames() : model.getDeclaredCollectionNames();
////         for (String collectionPropName : propertyNames) {
////            Class<?> collectionElementType = model.getCollectionElementType(collectionPropName);
////            if (entitySetNameByClass.get(collectionElementType) != null) {
////               // this will be a nav prop
////               continue;
////            }
////
////            EdmType type = typeMapping.findEdmType(collectionElementType);
////            EdmType.Builder typeBuilder = null;
////            if (type == null) {
////               typeBuilder = findComplexTypeForClass(collectionElementType);
////            } else {
////               typeBuilder = EdmSimpleType.newBuilder(type);
////            }
////
////            if (typeBuilder == null) {
////               continue;
////            }
////
////            // either a simple or complex type.
////            EdmProperty.Builder ep = EdmProperty.newBuilder(collectionPropName)
////                  .setNullable(true)
////                  .setCollectionKind(EdmProperty.CollectionKind.Collection)
////                  .setType(typeBuilder);
////
////            if (decorator != null) {
////               ep.setDocumentation(decorator.getDocumentationForProperty(namespace, structuralTypename, collectionPropName));
////               ep.setAnnotations(decorator.getAnnotationsForProperty(namespace, structuralTypename, collectionPropName));
////            }
////            rt.add(ep);
////         }
////
////         return rt;
////      }
//
//   /**
//    * get the Edm namespace
//    *
//    * @return the Edm namespace
//    */
//   public String getNamespace() {
//      return namespace;
//   }
//
//   /**
//    * provides an override point for applications to add application specific EdmFunctions to their producer.
//    *
//    * @param schema    the EdmSchema.Builder
//    * @param container the EdmEntityContainer.Builder
//    */
//   protected void addFunctions(EdmSchema.Builder schema, EdmEntityContainer.Builder container) {
//      // overridable :)
//   }
//
//   public class InMemoryEntityInfo<TEntity> {
//
//      HashMap entitySetName;
//      Func1 entityTypeName;
//      PropertyModel[] keys;
//      String<TEntity> entityClass;
//      String<String<TEntity>> get;
//      Iterable<RequestContext, Class<TEntity>> getWithContext;
//      Object<Func, System<Iterable, Func1>> id;
//      String properties;
//      boolean hasStream;
//
//      public Object getEntitySetName() {
//         return entitySetName;
//      }
//
//      public HashMap getEntityTypeName() {
//         return entityTypeName;
//      }
//
//      public Func1[] getKeys() {
//         return keys;
//      }
//
//      public PropertyModel<TEntity> getEntityClass() {
//         return entityClass;
//      }
//
//      public Object<Class<TEntity>> getGet() {
//         return get;
//      }
//
//      public Func1<RequestContext, Iterable<TEntity>> getGetWithContext() {
//         System.out.println("Call from getGetWithContext method - return type was changed!!!");
//         return getWithContext;
//      }
//
//      public Func1<Object, HashMap<String, Object>> getId() {
//         return id;
//      }
//
//      public PropertyModel getPropertyModel() {
//         return properties;
//      }
//
//      public boolean getHasStream() {
//         return hasStream;
//      }
//
//      public Class<?> getSuperClass() {
//         return entityClass.getSuperclass() != null && !entityClass.getSuperclass().equals(Object.class) ? entityClass.getSuperclass() : null;
//      }
//   }
//
//
//   public static class RequestContext {
//
//      public enum RequestType {
//         GetEntity, GetEntities, GetEntitiesCount, GetNavProperty
//      }
//
//      ;
//
//      public final RequestType requestType;
//      private final String entitySetName;
//      private OEntityKey entitySet;
//      private final QueryInfo navPropName;
//      private final PropertyPathHelper entityKey;
//      private final String queryInfo;
//      private final EdmEntitySet pathHelper;
//
//      public RequestType getRequestType() {
//         return requestType;
//      }
//
//      public String getEntitySetName() {
//         return entitySetName;
//      }
//
//      public OEntityKey getEntitySet() {
//         return entitySet;
//      }
//
//      public QueryInfo getNavPropName() {
//         return navPropName;
//      }
//
//      public PropertyPathHelper getEntityKey() {
//         return entityKey;
//      }
//
//      public QueryInfo getQueryInfo() {
//         return queryInfo;
//      }
//
//      public PropertyPathHelper getPathHelper() {
//         return pathHelper;
//      }
//
//      public static Builder newBuilder(RequestType requestType) {
//         return new Builder().requestType(requestType);
//      }
//
//      public static class Builder {
//
//         private RequestType requestType;
//         private String entitySetName;
//         private EdmEntitySet entitySet;
//         private String navPropName;
//         private OEntityKey entityKey;
//         private QueryInfo queryInfo;
//         private PropertyPathHelper pathHelper;
//
//         public Builder requestType(RequestType value) {
//            this.requestType = value;
//            return this;
//         }
//
//         public Builder entitySetName(String value) {
//            this.entitySetName = value;
//            return this;
//         }
//
//         public Builder entitySet(EdmEntitySet value) {
//            this.entitySet = value;
//            return this;
//         }
//
//         public Builder navPropName(String value) {
//            this.navPropName = value;
//            return this;
//         }
//
//         public Builder entityKey(OEntityKey value) {
//            this.entityKey = value;
//            return this;
//         }
//
//         public Builder queryInfo(QueryInfo value) {
//            this.queryInfo = value;
//            return this;
//         }
//
//         public Builder pathHelper(PropertyPathHelper value) {
//            this.pathHelper = value;
//            return this;
//         }
//
//         public RequestContext build() {
//            return new RequestContext(requestType, entitySetName, entitySet, navPropName, entityKey, queryInfo, pathHelper);
//         }
//      }
//
//      private RequestContext(RequestType requestType, String entitySetName, EdmEntitySet entitySet,
//                             String navPropName, OEntityKey entityKey, QueryInfo queryInfo, PropertyPathHelper pathHelper) {
//         this.requestType = requestType;
//         this.entitySetName = entitySetName;
//         this.entitySet = entitySet;
//         this.navPropName = navPropName;
//         this.entityKey = entityKey;
//         this.queryInfo = queryInfo;
//         this.pathHelper = pathHelper;
//      }
//   }
//
//
//}
