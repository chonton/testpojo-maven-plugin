package org.honton.chas.testpojo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreType;

import lombok.Data;

public class NotSerializableTest {

    @JsonIgnoreType
    @Data
    static public class NotSerializable {
    }
    
    @Before
    public void initialize() {
        PojoClassTester.setClassLoader(getClass().getClassLoader());        
    }
    
    @Test
    public void testNotSerializable() throws Exception {
        PojoClass pc = PojoClass.from(NotSerializable.class);
        Assert.assertFalse(pc.isJacksonSerializable());

        Assert.assertTrue(new PojoClassTester(NotSerializable.class.getName()).test());
    }
}
