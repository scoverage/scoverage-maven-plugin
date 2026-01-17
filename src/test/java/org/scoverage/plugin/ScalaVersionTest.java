/*
 * Copyright 2014-2026 Grzegorz Slowikowski (gslowikowski at gmail dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.scoverage.plugin;

import org.junit.Test;

import static org.junit.Assert.*;

public class ScalaVersionTest {

    @Test
    public void testInvalidVersion() {
        try {
            new ScalaVersion("whatever");
            fail("Should have thrown an exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid Scala version [whatever]. Expected major.minor(.bugfix)(modifier)", e.getMessage());
        }
    }

    @Test
    public void testIsAtLeast() {
        assertTrue(new ScalaVersion("2.13.12").isAtLeast("1.0"));
        assertTrue(new ScalaVersion("2.12.1").isAtLeast("1.9"));
        assertTrue(new ScalaVersion("2.13.10").isAtLeast("2.0"));
        assertTrue(new ScalaVersion("2.13.1").isAtLeast("2.13"));
        assertTrue(new ScalaVersion("2.10.0").isAtLeast("2.10.0-rc8"));
        assertTrue(new ScalaVersion("2.13.1").isAtLeast("2.13.0"));
        assertTrue(new ScalaVersion("2.13.12").isAtLeast("2.13.12"));
        assertFalse(new ScalaVersion("3.3.1").isAtLeast("3.3.2-rc1"));
        assertFalse(new ScalaVersion("2.12.10").isAtLeast("2.13"));
        assertFalse(new ScalaVersion("2.13.12").isAtLeast("3.0"));
        assertFalse(new ScalaVersion("2.13.12").isAtLeast("3.0.0"));
    }

    @Test
    public void testFull() {
        assertEquals("2.12.2", new ScalaVersion("2.12.2").full);
        assertEquals("2.13.12", new ScalaVersion("2.13.12").full);
        assertEquals("3.0.0", new ScalaVersion("3.0.0").full);
        assertEquals("3.4.0-RC1", new ScalaVersion("3.4.0-RC1").full);
    }

    @Test
    public void testCompatible() {
        assertEquals("2.12", new ScalaVersion("2.12.2").compatible);
        assertEquals("2.12", new ScalaVersion("2.12.18").compatible);
        assertEquals("2.13", new ScalaVersion("2.13.0").compatible);
        assertEquals("2.13", new ScalaVersion("2.13.12").compatible);
        assertEquals("3", new ScalaVersion("3.0.0").compatible);
        assertEquals("3", new ScalaVersion("3.3.1").compatible);
        assertEquals("3", new ScalaVersion("3.4.0").compatible);
        assertEquals("3.4.0-RC1", new ScalaVersion("3.4.0-RC1").compatible);
    }

    @Test
    public void testIsScala2() {
        assertTrue(new ScalaVersion("2.12.2").isScala2());
        assertTrue(new ScalaVersion("2.13.12").isScala2());
        assertFalse(new ScalaVersion("3.0.0").isScala2());
        assertFalse(new ScalaVersion("3.4.0-RC1").isScala2());
        assertFalse(new ScalaVersion("1.0.0").isScala2());
    }

}