package me.lorenzo0111.elections.api.objects;

import java.util.concurrent.CompletableFuture;

import me.lorenzo0111.elections.database.Version;

public interface ICacheEntry {
    public String getName();
    public CompletableFuture<Boolean> delete();
    public CompletableFuture<Boolean> update();

    public Version version();
}
