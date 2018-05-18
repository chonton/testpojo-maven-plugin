package org.honton.chas.testpojo;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.joda.time.DateTime;

import com.google.common.collect.ImmutableList;

import java.time.ZonedDateTime;

import lombok.Data;

@Data
public class DataPojo {
    static public String ignoreThis;

    @Accessors(chain = true)
    private String s;
    private int i;
    private ValuePojo p;
    private HashMap<String,String> hm;
    private Collection<String> cs;
    private List<String> ls;
    private Map<String,String> mss;
    private ImmutableList<Long> il;
    private TimeUnit timeUnit;
    private DateTime dateTime;
    private ZonedDateTime zoneDateTime;

    public DataPojo nonStaticBuilderToIgnore() {
        throw new IllegalStateException();
    }

    static DataPojo nonPublicStaticBuilderToIgnore() {
        throw new IllegalStateException();
    }

    static public DataPojo publicStaticBuilderWithParametersToIgnore(DataPojo x) {
        throw new IllegalStateException();
    }

    DataPojo toBuilder() {
        throw new IllegalStateException();
    }
}
