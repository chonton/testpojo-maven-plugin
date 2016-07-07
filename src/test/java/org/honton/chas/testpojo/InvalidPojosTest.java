package org.honton.chas.testpojo;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import lombok.Data;

public class InvalidPojosTest {

    static AtomicInteger counter = new AtomicInteger();
    
    @Data
    static public class BadEqualsFalse {
        private int i;

        @Override
        public boolean equals(Object obj) {
            return false;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + i;
            return result;
        }
    }
    
    @Data
    static public class BadEqualsTrue {
        private int i;

        @Override
        public boolean equals(Object obj) {
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + i;
            return result;
        }

    }

    @Data
    static public class BadHash {
        private int i;

        @Override
        public boolean equals(Object obj) {
            return i == ((BadHash)obj).i;
        }
        
        @Override
        public int hashCode() {
            return counter.incrementAndGet();
        }
    }

    @Before
    public void initialize() {
        PojoClassTester.setClassLoader(getClass().getClassLoader());
    }

    @Test
    public void testBadEqualsTrue() throws Exception {
        Assert.assertFalse(new PojoClassTester(BadEqualsTrue.class.getName()).test());
    }

    @Test
    public void testBadEqualsFalse() throws Exception {
        Assert.assertFalse(new PojoClassTester(BadEqualsFalse.class.getName()).test());
    }

    @Test
    public void testBadHash() throws Exception {
        Assert.assertFalse(new PojoClassTester(BadHash.class.getName()).test());
    }

}
