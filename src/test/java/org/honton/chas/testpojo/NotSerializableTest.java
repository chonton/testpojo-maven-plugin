package org.honton.chas.testpojo;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Data;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NotSerializableTest {
    @JsonIgnoreType
    public static class JsonIgnoreTypeClass {
    }

    @Data
    public static class NotSerializable {
        public NotSerializable(Void param) {
        }
    }
    
    @Before
    public void initialize() {
        PojoClassTester.setClassLoader(getClass().getClassLoader());        
    }
    
    @Test
    public void testNotSerializable() throws Exception {
        Object value = PojoClass.from(NotSerializable.class).createVariant(-1);
        Assert.assertFalse(PojoClass.isJacksonSerializable(value));

        Assert.assertTrue(new PojoClassTester(NotSerializable.class.getName()).test());
    }

    @Test
    public void testJsonIgnoreTypeClass() throws Exception {
        Object value = PojoClass.from(JsonIgnoreTypeClass.class).createVariant(-1);
        Assert.assertFalse(PojoClass.isJacksonSerializable(value));

        Assert.assertTrue(new PojoClassTester(JsonIgnoreTypeClass.class.getName()).test());
    }
}
