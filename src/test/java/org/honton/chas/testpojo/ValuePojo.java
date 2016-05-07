package org.honton.chas.testpojo;

import java.util.Collection;
import java.util.HashMap;

import lombok.Value;

@Value
public class ValuePojo {
    private final String s;
    private final int i;
    private DataPojo p;
    private HashMap<String,String> hm;
    private Collection<String> cs;
}
