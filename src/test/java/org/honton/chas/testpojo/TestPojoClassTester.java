package org.honton.chas.testpojo;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestPojoClassTester {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { 
            { DataPojo.class.getCanonicalName() }, 
            { BuilderPojo.class.getCanonicalName() },
            { ToBuilderPojo.class.getCanonicalName() },
            { ValuePojo.class.getCanonicalName() } }
        );
    }

    @Parameter
    public String pojoClassName;

    @Test
    public void testData() throws Exception {
        new PojoClassTester(getClass().getClassLoader(), pojoClassName).test();
    }
}
