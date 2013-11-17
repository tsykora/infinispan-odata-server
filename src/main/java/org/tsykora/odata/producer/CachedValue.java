package org.tsykora.odata.producer;

import java.io.Serializable;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * Instances of this class will be directly stored into Infinispan cache.
 * They encapsulate JsonValueWrapper containing JSON as a String.
 *
 * @author tsykora@redhat.com
 */
@Indexed
public class CachedValue implements Serializable {
    @Field(analyze = Analyze.YES, store = Store.YES)
    @FieldBridge(impl = JsonValueWrapperFieldBridge.class)
    JsonValueWrapper json;

    public CachedValue(String json) {
        this.json = new JsonValueWrapper(json);
    }

    public JsonValueWrapper getJsonValueWrapper() {
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CachedValue that = (CachedValue) o;
        if (!json.equals(that.json)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return json.hashCode();
    }

    @Override
    public String toString() {
        return "CachedValue{" +
                "json=" + json +
                '}';
    }
}