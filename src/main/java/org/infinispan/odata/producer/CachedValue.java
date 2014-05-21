package org.infinispan.odata.producer;

import java.io.Serializable;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;

/**
 * Instances of this class will be directly stored into Infinispan cache.
 * They encapsulate JsonValueWrapper containing JSON document as a String.
 *
 * Not necessary Hibernate search options of @Field annotation are disabled.
 *
 * Hibernate search documentation tip:
 * Whether or not you want to analyze a property depends on whether you wish to search
 * the element as is, or by the words it contains.
 * It make sense to analyze a text field, but probably not a date field.
 *
 * @author Tomas Sykora <tomas@infinispan.org>
 */
@Indexed
public class CachedValue implements Serializable {

    @Field(analyze = Analyze.YES, store = Store.NO,
            norms = Norms.NO, termVector = TermVector.NO)
    @FieldBridge(impl = JsonValueWrapperFieldBridge.class)
    JsonValueWrapper jsonWrapper;

    public CachedValue(String json) {
        this.jsonWrapper = new JsonValueWrapper(json);
    }

    public JsonValueWrapper getJsonValueWrapper() {
        return jsonWrapper;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CachedValue that = (CachedValue) o;
        if (!jsonWrapper.equals(that.jsonWrapper)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return jsonWrapper.hashCode();
    }

    @Override
    public String toString() {
        return "CachedValue{" + jsonWrapper + "}";
    }
}