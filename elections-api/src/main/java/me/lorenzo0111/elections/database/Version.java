package me.lorenzo0111.elections.database;

public class Version {
    private Integer version;
    private Integer last;

    public Version(boolean dirty) {
        this.last = 0;
        if(dirty) {
            this.version = 1;
        } else {
            this.version = 0;
        }
    }

    public Integer getVersion() {
        return this.version;
    }

    public Integer getLast() {
        return this.last;
    }

    public void setLast(Integer last) {
        this.last = last;
    }

    public void dirty() {
        this.version++;
    }

    public boolean isDirty() {
        return this.version > this.last;
    }
}
