package pkg03

import org.junit.Test;
import org.junit.Assert.assertEquals

class HelloServiceTest
{
    @Test
    def test3()
    {
        assertEquals("Hello from module 3", HelloService3.hello)
    }

}
