package org.honton.chas.testpojo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AbstractClassTest {

    public static abstract class AbstractClass {
    }

    @Before
    public void initialize() {
        PojoClassTester.setClassLoader(getClass().getClassLoader());
    }

    @Test
    public void testAbstract() throws Exception {
        PojoClass pc = PojoClass.from(AbstractClass.class);
        Assert.assertFalse(pc.isConstructable());

        Assert.assertTrue(new PojoClassTester(AbstractClass.class.getName()).test());
    }
}
