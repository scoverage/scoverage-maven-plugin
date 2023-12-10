package service

import org.junit.Test;
import org.junit.Assert.assertEquals

class HelloServiceScalaTest
{
    @Test
    def test1() = assertEquals("Hello", HelloServiceScala.hello)

}
