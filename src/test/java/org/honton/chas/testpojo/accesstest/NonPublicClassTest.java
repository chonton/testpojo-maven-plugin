package org.honton.chas.testpojo.accesstest;

import org.honton.chas.testpojo.PojoClass;
import org.honton.chas.testpojo.PojoClassTester;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import lombok.Data;

public class NonPublicClassTest {

    @Data
    private static class PrivateClass {
    }

    @Data
    protected static class ProtectedClass {
    }

    @Data
    static class PackageProtectedClass {
    }

    @Data
    class MemberClass {
    }

    @Before
    public void initialize() {
        PojoClassTester.setClassLoader(getClass().getClassLoader());
    }

    @Test
    public void testPrivate() throws Exception {
        PojoClass pc = PojoClass.from(PrivateClass.class);
        Assert.assertTrue(pc.isConstructable());

        Assert.assertTrue(new PojoClassTester(PrivateClass.class.getName()).test());
    }

    @Test
    public void testProtected() throws Exception {
        PojoClass pc = PojoClass.from(ProtectedClass.class);
        Assert.assertTrue(pc.isConstructable());

        Assert.assertTrue(new PojoClassTester(ProtectedClass.class.getName()).test());
    }

    @Test
    public void testPackageProtected() throws Exception {
        PojoClass pc = PojoClass.from(PackageProtectedClass.class);
        Assert.assertTrue(pc.isConstructable());

        Assert.assertTrue(new PojoClassTester(PackageProtectedClass.class.getName()).test());
    }

    @Test
    public void testInner() throws Exception {
        PojoClass pc = PojoClass.from(MemberClass.class);
        Assert.assertFalse(pc.isConstructable());

        Assert.assertTrue(new PojoClassTester(MemberClass.class.getName()).test());
    }
}
