package org.tsykora.odata.consumer;

import org.infinispan.api.BasicCache;
import org.infinispan.util.concurrent.NotifyingFuture;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.core.OProperties;
import org.tsykora.odata.common.Utils;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * ODataCache implements BasicCache API. This provide access to Service endpoint
 * and caches running on side of Producer.
 *
 * @author tsykora
 */
public class ODataCache<K, V> implements BasicCache<K, V> {

    private final String cacheName;
    private final ODataConsumer consumer;

    /**
     * @param consumer - consumer instance
     * @param cacheName - string describing cache name (and EntitySet)
     */
    public ODataCache(ODataConsumer consumer, String cacheName) {
        this.consumer = consumer;
        this.cacheName = cacheName;
    }

    @Override
    public V get(Object key) {
        V v = null;
        v = (V) consumer.getEntity(cacheName, key).execute().getProperty("Value").getValue();
        // I have got here OEntity with serialized data, I need to deserialize them
        v = (V) Utils.deserialize((byte[]) v);
        return v;
    }

    /**
     * Working ONLY for STRING key and value now!
     * <p/>
     * TODO: write code more economically
     *
     * @param key
     * @param value
     * @return value from successfully executed OEntity request
     */
    @Override
    public V put(Object key, Object value) {
        V v = null;

        v = (V) consumer.createEntity(cacheName).
                properties(OProperties.binary("Key", Utils.serialize(key))).
                properties(OProperties.binary("Value", Utils.serialize(value))).
                execute().getProperty("Value").getValue(); // OEntity.getProperty()
        // I have got here OEntity with serialized data, I need to deserialize them
        v = (V) Utils.deserialize((byte[]) v);
        return v;
    }

    /**
     * @return cacheName which is the same as EntitySet (cache unique
     * identifier)
     */
    @Override
    public String getName() {
        return cacheName;
    }

    // ****************************************************************
    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object put(Object key, Object value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object put(Object key, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getVersion() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object putIfAbsent(Object key, Object value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object replace(Object key, Object value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object putIfAbsent(Object key, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object replace(Object key, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Object> putAsync(Object key, Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Object> putAsync(Object key, Object value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Object> putAsync(Object key, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Void> clearAsync() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Object> putIfAbsentAsync(Object key, Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Object> putIfAbsentAsync(Object key, Object value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Object> putIfAbsentAsync(Object key, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<V> removeAsync(Object key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Boolean> removeAsync(Object key, Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Object> replaceAsync(Object key, Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Object> replaceAsync(Object key, Object value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Object> replaceAsync(Object key, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(Object key, Object oldValue, Object newValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(Object key, Object oldValue, Object newValue, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(Object key, Object oldValue, Object newValue, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Object> getAsync(Object key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object newValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object replace(Object key, Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
