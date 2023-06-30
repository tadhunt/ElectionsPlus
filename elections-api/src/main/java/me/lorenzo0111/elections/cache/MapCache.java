/*
 * This file is part of ElectionsPlus, licensed under the MIT License.
 *
 * Copyright (c) Lorenzo0111
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.lorenzo0111.elections.cache;

import me.lorenzo0111.elections.api.objects.Cache;
import me.lorenzo0111.elections.api.objects.ICacheEntry;
import me.lorenzo0111.elections.database.Version;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class MapCache<K, V extends ICacheEntry> implements Cache<K, V> {
    private final Map<K, V> cache = new ConcurrentHashMap<>();
    private final Map<K, V> delete = new ConcurrentHashMap<>();
    private Logger logger;

    MapCache(Logger logger) {
        this.logger = logger;
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public void reset() {
        for (K key : cache.keySet()) {
            V value = cache.remove(key);
            delete.put(key, value);
        }
    }

    @Override
    public void add(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public boolean remove(K key, V value) {
        boolean removed = cache.remove(key, value);
        if (removed) {
            delete.put(key, value);
        }

        return removed;
    }

    @Override
    public V remove(K key) {
        V value;

        value = cache.remove(key);
        if (value != null) {
            delete.put(key, value);
        }

        return value;
    }

    @Override
    public V get(K key) {
        return cache.get(key);
    }

    @Override
    public Map<K, V> map() {
        return cache;
    }

    @Override
    public void persist() {
        for (V value : delete.values()) {
logger.severe("deleting " + value.getName());
            value.delete();
        }

        for (V value : cache.values()) {
            Version version = value.version();
            Integer currentVersion = version.getVersion();
            Integer lastVersion = version.getLast();

            if (currentVersion > lastVersion) {
logger.severe("updating " + value.getName());
                value.update()
                    .thenAccept((success) -> {
                        if (success) {
                            value.version().setLast(currentVersion);
                        }
                    });
            }
        }
    }

    @Override
    public V findByName(String name) {
        for(V value : cache.values()) {
            String vname = value.getName();
            if (vname != null && vname.equals(name)) {
                return value;
            }
        }

        return null;
    }
}