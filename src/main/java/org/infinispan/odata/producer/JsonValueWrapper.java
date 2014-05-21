package org.infinispan.odata.producer;

import java.io.Serializable;

/**
 * Class is used to wrap JSON values coming from clients (OData consumers). JsonValueWrapperFieldBridge is used as
 * a bridge which allows indexing of the JSON payload.
 *
 * @author Tomas Sykora <tomas@infinispan.org>
 */
public final class JsonValueWrapper implements Serializable {

    private final String json;
    private int hashCode = 0;

    public JsonValueWrapper(String json) {
        this.json = json;
    }

    public String getJson() {
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JsonValueWrapper that = (JsonValueWrapper) o;

        if (hashCode != that.hashCode) return false;
        if (!json.equals(that.json)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = json.hashCode();
        result = 31 * result + hashCode;
        return result;
    }

    @Override
    public String toString() {
        return "JsonValueWrapper(" + json + ")";
    }
}
