package pkg02_02

import org.junit.Test;
import org.junit.Assert.assertEquals

class HelloServiceTest
{
    @Test
    def test2()
    {
        assertEquals("Hello from submodule02_02", HelloService2.hello)
    }

}
