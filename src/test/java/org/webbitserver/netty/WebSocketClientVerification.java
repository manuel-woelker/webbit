package org.webbitserver.netty;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.webbitserver.BaseWebSocketHandler;
import org.webbitserver.WebServer;
import org.webbitserver.WebSocket;
import org.webbitserver.WebSocketConnection;
import samples.echo.EchoWsServer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class WebSocketClientVerification {
    private EchoWsServer server;
    private URI wsUri;

    @Before
    public void start() throws IOException, URISyntaxException, InterruptedException, ExecutionException {
        server = new EchoWsServer(59509);
        server.start();
        URI uri = server.uri();
        wsUri = new URI(uri.toASCIIString().replaceFirst("http", "ws"));
    }

    protected abstract WebServer createServer() throws IOException;

    protected abstract void configure(WebSocket ws);

    @After
    public void die() throws IOException, InterruptedException, ExecutionException {
        server.stop();
    }

    @Test
    public void server_echoes_1_byte_message_immediately() throws InterruptedException {
        assertEchoed(message(1));
    }

    @Test
    public void server_echoes_125_byte_message_immediately() throws InterruptedException {
        assertEchoed(message(125));
    }

    @Test
    public void server_echoes_126_byte_message_immediately() throws InterruptedException {
        assertEchoed(message(126));
    }

    @Test
    public void server_echoes_127_byte_message_immediately() throws InterruptedException {
        assertEchoed(message(127));
    }

    @Test
    public void server_echoes_128_byte_message_immediately() throws InterruptedException {
        assertEchoed(message(128));
    }

    // This always fails. We should un-Ignore this when #65 is fixed.
    @Ignore
    @Test
    public void server_echoes_0_byte_message_immediately() throws InterruptedException {
        assertEchoed(message(0));
    }

    private void assertEchoed(final String message) throws InterruptedException {
        final CountDownLatch countDown = new CountDownLatch(2);
        final List<String> received = Collections.synchronizedList(new ArrayList<String>());

        WebSocket ws = new WebSocketClient(wsUri, new BaseWebSocketHandler() {
            @Override
            public void onOpen(WebSocketConnection connection) throws Exception {
                connection.send(message);
                countDown.countDown();
            }

            @Override
            public void onMessage(WebSocketConnection connection, String msg) throws Throwable {
                received.add(msg);
                countDown.countDown();
            }
        }, Executors.newSingleThreadExecutor());
        configure(ws);
        ws.start();

        assertTrue("Message wasn't echoed", countDown.await(300, TimeUnit.MILLISECONDS));
        assertEquals(message, received.get(0));
    }

    private String message(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append("*");
        }
        return sb.toString();
    }
}