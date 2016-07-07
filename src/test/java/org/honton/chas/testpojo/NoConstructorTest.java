package org.honton.chas.testpojo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NoConstructorTest {

    static class NoPublicConstructor {
        NoPublicConstructor() {
        }
    }
  
    @Before
    public void initialize() {
        PojoClassTester.setClassLoader(getClass().getClassLoader());        
    }

    @Test
    public void testNoPublic() throws Exception {
        PojoClass pc = PojoClass.from(NoPublicConstructor.class);
        Assert.assertFalse(pc.isConstructable());

        Assert.assertTrue(new PojoClassTester(NoPublicConstructor.class.getName()).test());
    }
}
