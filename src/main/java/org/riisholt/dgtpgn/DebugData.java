package org.riisholt.dgtpgn;

import java.io.Serializable;

public class DebugData implements Serializable {
    private static final long serialVersionUID = 1L;
    public boolean input;
    public byte[] data;

    public DebugData(boolean input, byte[] data) {
        this.input = input;
        this.data = data;
    }
}
