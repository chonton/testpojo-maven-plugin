package org.honton.chas.testpojo;

import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PojoSpyTest {
    @Before
    public void initialize() {
        PojoClassTester.setClassLoader(getClass().getClassLoader());
    }

    @Test
    public void testData() throws Exception {
        assertTrue(new PojoClassTester(PojoSpy.class.getName()).test());

        assertEquals(EnumSet.allOf(Covered.class), PojoSpy.covered);
    }
}

enum Covered {
    CONSTRUCTOR,
    GETTER,
    SETTER,
    EQUALS,
    HASH_CODE,
    TO_STRING
}

@SuppressWarnings("ALL")
class PojoSpy {
    static EnumSet<Covered> covered = EnumSet.noneOf(Covered.class);
    private String value;

    public PojoSpy() {
        covered.add(Covered.CONSTRUCTOR);
    }

    public String getValue() {
        covered.add(Covered.GETTER);

        return value;
    }

    public void setValue(String value) {
        covered.add(Covered.SETTER);

        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        covered.add(Covered.EQUALS);

        if (this == o) return true;
        else if (o == null || getClass() != o.getClass()) return false;
        PojoSpy pojoSpy = (PojoSpy) o;
        return Objects.equals(value, pojoSpy.value);
    }

    @Override
    public int hashCode() {
        covered.add(Covered.HASH_CODE);

        return Objects.hash(value);
    }

    @Override
    public String toString() {
        covered.add(Covered.TO_STRING);

        return "PojoSpy{value='" + value + "'}";
    }
}

