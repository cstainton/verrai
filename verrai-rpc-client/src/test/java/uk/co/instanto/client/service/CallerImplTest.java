package uk.co.instanto.client.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CallerImplTest {

    interface TestService {
        String echo(String input);
    }

    @Mock
    private TestService serviceStub;

    private CallerImpl<TestService> caller;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        caller = new CallerImpl<>(serviceStub);
    }

    @Test
    public void testGetService() {
        assertEquals(serviceStub, caller.getService());
    }

    @Test
    public void testCall() {
        String input = "testInput";
        String expected = "echo: testInput";

        when(serviceStub.echo(input)).thenReturn(expected);

        String result = caller.call(service -> service.echo(input));

        assertEquals(expected, result);
        verify(serviceStub).echo(input);
    }
}
