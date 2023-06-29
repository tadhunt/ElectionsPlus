package me.lorenzo0111.elections.api.objects;

import java.util.concurrent.CompletableFuture;

public interface ICacheEntry {
    public String getName();
    public CompletableFuture<Boolean> delete();
    public CompletableFuture<Boolean> update();
    public boolean dirty();
    public void clean();
}
