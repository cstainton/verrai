package dev.verrai.rpc.common.serialization;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.Map;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JsonCodecRegistryTest {

    @Before
    public void setUp() throws Exception {
        clearRegistry();
    }

    @After
    public void tearDown() throws Exception {
        clearRegistry();
    }

    private void clearRegistry() throws Exception {
        Field codecsField = JsonCodecRegistry.class.getDeclaredField("codecs");
        codecsField.setAccessible(true);
        ((Map<?, ?>) codecsField.get(null)).clear();
    }

    @Test
    public void registerAndGet() {
        @SuppressWarnings("unchecked")
        JsonCodec<String> codec = mock(JsonCodec.class);
        JsonCodecRegistry.register(String.class, codec);
        assertSame(codec, JsonCodecRegistry.get(String.class));
    }

    @Test
    public void getUnregistered() {
        assertNull(JsonCodecRegistry.get(Integer.class));
    }

    @Test
    public void registerOverwrite() {
        @SuppressWarnings("unchecked")
        JsonCodec<String> codec1 = mock(JsonCodec.class);
        @SuppressWarnings("unchecked")
        JsonCodec<String> codec2 = mock(JsonCodec.class);

        JsonCodecRegistry.register(String.class, codec1);
        assertSame(codec1, JsonCodecRegistry.get(String.class));

        JsonCodecRegistry.register(String.class, codec2);
        assertSame(codec2, JsonCodecRegistry.get(String.class));
    }

    @Test
    public void registerNullKey() {
        @SuppressWarnings("unchecked")
        JsonCodec<String> codec = mock(JsonCodec.class);
        // HashMap allows null keys.
        // We test this behavior to ensure stability if someone registers with null.
        try {
            JsonCodecRegistry.register(null, codec);
            assertSame(codec, JsonCodecRegistry.get(null));
        } catch (Exception e) {
            fail("Should not throw exception for null key registration if using HashMap");
        }
    }

    @Test
    public void registerNullValue() {
        // HashMap allows null values.
        try {
            JsonCodecRegistry.register(String.class, null);
            assertNull(JsonCodecRegistry.get(String.class));
        } catch (Exception e) {
             fail("Should not throw exception for null value registration if using HashMap");
        }
    }

    @Test
    public void registerDifferentClasses() {
        @SuppressWarnings("unchecked")
        JsonCodec<String> stringCodec = mock(JsonCodec.class);
        @SuppressWarnings("unchecked")
        JsonCodec<Integer> integerCodec = mock(JsonCodec.class);

        JsonCodecRegistry.register(String.class, stringCodec);
        JsonCodecRegistry.register(Integer.class, integerCodec);

        assertSame(stringCodec, JsonCodecRegistry.get(String.class));
        assertSame(integerCodec, JsonCodecRegistry.get(Integer.class));
    }
}
