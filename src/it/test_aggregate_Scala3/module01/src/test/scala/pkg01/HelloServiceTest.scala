package pkg01

import org.junit.Test;
import org.junit.Assert.assertEquals

class HelloServiceTest
{
    @Test
    def test1(): Unit =
    {
        assertEquals("Hello from module 1", HelloService1.hello)
    }

}
