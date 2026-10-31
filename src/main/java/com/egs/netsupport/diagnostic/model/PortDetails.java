package com.egs.netsupport.diagnostic.model;

public class PortDetails {

    public boolean linkUp;
    public int speedMbps;
    public long inputErrors;
    public long crcErrors;
    public long outputErrors;
    public long collisions;
    public long outputDrops;

    @Override
    public String toString() {
        return "PortDetails{" +
                "linkUp=" + linkUp +
                ", speedMbps=" + speedMbps +
                ", crcErrors=" + crcErrors +
                ", outputDrops=" + outputDrops +
                '}';
    }
}
