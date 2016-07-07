package org.honton.chas.testpojo;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PojoClassTest {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { 
            { DataPojo.class.getName() }, 
            { BuilderPojo.class.getName() },
            { ToBuilderPojo.class.getName() },
            { ValuePojo.class.getName() }
        }
        );
    }

    @Parameter
    public String pojoClassName;
    
    @Before
    public void initialize() {
        PojoClassTester.setClassLoader(getClass().getClassLoader());        
    }

    @Test
    public void testData() throws Exception {
        Assert.assertTrue(new PojoClassTester(pojoClassName).test());
    }
}
