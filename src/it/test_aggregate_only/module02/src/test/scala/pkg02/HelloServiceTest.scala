package pkg02

import org.junit.Test;
import org.junit.Assert.assertEquals

class HelloServiceTest
{
    @Test
    def test2()
    {
        assertEquals("Hello from module 2", HelloService2.hello)
    }

}
