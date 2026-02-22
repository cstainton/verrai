package dev.verrai.rpc.common.serialization;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReflectionFreeJsonSerializerTest {

    private ReflectionFreeJsonSerializer serializer;

    @Before
    public void setUp() {
        serializer = new ReflectionFreeJsonSerializer();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecode_withNullClass_throwsException() {
        serializer.decode(new byte[]{123, 125}, null);
    }

    @Test(expected = RuntimeException.class)
    public void testDecode_withUnregisteredClass_throwsException() {
        serializer.decode(new byte[]{123, 125}, UnregisteredClass.class);
    }

    @Test
    public void testDecode_withRegisteredClass_returnsObject() {
        JsonCodec<TestObject> mockCodec = mock(JsonCodec.class);
        TestObject expectedObject = new TestObject();
        when(mockCodec.fromJson(anyString())).thenReturn(expectedObject);

        JsonCodecRegistry.register(TestObject.class, mockCodec);

        TestObject result = serializer.decode("{}".getBytes(), TestObject.class);
        assertEquals(expectedObject, result);
    }

    @Test
    public void testDecode_withNullBytes_returnsNull() {
        assertNull(serializer.decode(null, TestObject.class));
    }

    @Test
    public void testDecode_withEmptyBytes_returnsNull() {
        assertNull(serializer.decode(new byte[0], TestObject.class));
    }

    private static class UnregisteredClass {}
    private static class TestObject {}
}
