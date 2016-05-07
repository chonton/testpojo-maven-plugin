package org.honton.chas.testpojo;

import java.util.Collection;
import java.util.HashMap;

import lombok.Data;

@Data
public class DataPojo {
    private String s;
    private int i;
    private ValuePojo p;
    private HashMap<String,String> hm;
    private Collection<String> cs;
}
