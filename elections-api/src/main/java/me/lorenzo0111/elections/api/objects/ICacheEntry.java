package me.lorenzo0111.elections.api.objects;

public interface ICacheEntry {
    public String getName();
    public void delete();
    public void update();
    public boolean dirty();
}
