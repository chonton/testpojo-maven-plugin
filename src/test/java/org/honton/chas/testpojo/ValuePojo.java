package org.honton.chas.testpojo;

import java.util.Collection;
import java.util.HashMap;

import lombok.Value;

@Value
public class ValuePojo {
    private final String s;
    private final int i;
    private final Long l;
    private final DataPojo p;
    private final HashMap<String,String> hm;
    private final Collection<String> cs;
    private final NoValues nv;

    public enum NoValues {
    }
}
