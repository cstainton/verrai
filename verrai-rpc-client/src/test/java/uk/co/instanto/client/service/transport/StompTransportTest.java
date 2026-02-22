package uk.co.instanto.client.service.transport;

import dev.verrai.rpc.common.transport.MessageHandler;
import dev.verrai.rpc.common.transport.stomp.StompClient;
import dev.verrai.rpc.common.transport.stomp.StompMessage;
import dev.verrai.rpc.common.transport.stomp.StompSubscriptionCallback;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Base64;
import java.util.Collections;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

public class StompTransportTest {

    @Mock
    private StompClient stompClient;

    @Mock
    private MessageHandler messageHandler;

    @Captor
    private ArgumentCaptor<StompSubscriptionCallback> callbackCaptor;

    private StompTransport stompTransport;
    private final String sendDest = "/app/send";
    private final String subDest = "/topic/sub";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        stompTransport = new StompTransport(stompClient, sendDest, subDest);
        stompTransport.addMessageHandler(messageHandler);
    }

    @Test
    public void testConstructorRegistersCallback() {
        verify(stompClient).subscribe(eq(subDest), any(StompSubscriptionCallback.class));
    }

    @Test
    public void testSendWhenConnected() {
        when(stompClient.isConnected()).thenReturn(true);
        byte[] data = "Hello".getBytes();
        stompTransport.send(data);

        String expectedBase64 = Base64.getEncoder().encodeToString(data);
        verify(stompClient).send(eq(sendDest), eq(expectedBase64));
    }

    @Test
    public void testSendWhenNotConnected() {
        when(stompClient.isConnected()).thenReturn(false);
        byte[] data = "Hello".getBytes();
        stompTransport.send(data);

        verify(stompClient, never()).send(any(), any());
    }

    @Test
    public void testReceiveValidMessage() {
        verify(stompClient).subscribe(eq(subDest), callbackCaptor.capture());
        StompSubscriptionCallback callback = callbackCaptor.getValue();

        StompMessage message = mock(StompMessage.class);
        byte[] data = "Hello World".getBytes();
        String base64 = Base64.getEncoder().encodeToString(data);

        when(message.getBody()).thenReturn(base64);
        when(message.getHeaders()).thenReturn(Collections.singletonMap("destination", subDest));

        callback.onMessage(message);

        verify(messageHandler).onMessage(argThat(argument ->
            Arrays.equals(argument, data)
        ));
    }

    @Test
    public void testReceiveInvalidBase64() {
        verify(stompClient).subscribe(eq(subDest), callbackCaptor.capture());
        StompSubscriptionCallback callback = callbackCaptor.getValue();

        StompMessage message = mock(StompMessage.class);
        when(message.getBody()).thenReturn("NotBase64%%%");
        when(message.getHeaders()).thenReturn(Collections.emptyMap());

        callback.onMessage(message);

        verify(messageHandler, never()).onMessage(any());
    }

    @Test
    public void testReceiveNullBody() {
        verify(stompClient).subscribe(eq(subDest), callbackCaptor.capture());
        StompSubscriptionCallback callback = callbackCaptor.getValue();

        StompMessage message = mock(StompMessage.class);
        when(message.getBody()).thenReturn(null);
        when(message.getHeaders()).thenReturn(Collections.emptyMap());

        callback.onMessage(message);

        verify(messageHandler, never()).onMessage(any());
    }

    @Test
    public void testHandlerExceptionIsolation() {
        verify(stompClient).subscribe(eq(subDest), callbackCaptor.capture());
        StompSubscriptionCallback callback = callbackCaptor.getValue();

        MessageHandler failHandler = mock(MessageHandler.class);
        doThrow(new RuntimeException("Fail")).when(failHandler).onMessage(any());

        stompTransport.addMessageHandler(failHandler);

        StompMessage message = mock(StompMessage.class);
        byte[] data = "Hello".getBytes();
        String base64 = Base64.getEncoder().encodeToString(data);

        when(message.getBody()).thenReturn(base64);
        when(message.getHeaders()).thenReturn(Collections.emptyMap());

        callback.onMessage(message);

        verify(failHandler).onMessage(any());
        verify(messageHandler).onMessage(any());
    }
}
