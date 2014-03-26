package io.github.arnos.httpproxy;
/*
 * Copyright 2014, Arno Schulz
 *
 * This file is licensed under the MIT License (MIT) (see License file)
 *
 * @author <a href="https://arnos.github.io">Arno Schulz</a>
 */

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.platform.Verticle;
import org.vertx.java.core.streams.Pump;
import redis.clients.jedis.Jedis;

public class Worker extends Verticle {
    
    private HttpServer server;
    private JsonObject config;
    private Jedis jedis;
    
    private void handle(final WebSocket ws) {
        //determine the redirect
        jedis.asking();
        
        HttpClient client = vertx.createHttpClient().setHost("foo.com");
        
        HttpClient cHttp = client.connectWebsocket("/some-uri", new Handler<WebSocket>() {
            public void handle(final WebSocket rWS) {
                final Pump in = Pump.createPump(ws, rWS).start();
                final Pump out = Pump.createPump(rWS, ws).start();
                ws.endHandler(new VoidHandler() {
                    public void handle() {
                        in.stop();
                        out.stop();
                        rWS.close();
                    }
                });
            }
        });
    }
    
    private void handle(final HttpServerRequest req) {
        //determine the redirect
        jedis.asking();
        
        HttpClient client = vertx.createHttpClient().setHost("foo.com");
        
        final HttpClientRequest cReq = client.request(req.method(), req.uri().substring(5), new Handler<HttpClientResponse>() {
            public void handle(HttpClientResponse cRes) {
                //System.out.println("Proxying response: " + cRes.statusCode());
                req.response().setStatusCode(cRes.statusCode());
                req.response().headers().set(cRes.headers());
                req.response().setChunked(true);
                cRes.dataHandler(new Handler<Buffer>() {
                    public void handle(Buffer data) {
                        //System.out.println("Proxying response body:" + data);
                        req.response().write(data);
                    }
                });
                cRes.endHandler(new VoidHandler() {
                    public void handle() {
                        req.response().end();
                    }
                });
            }
        });
        cReq.headers().set(req.headers());
        cReq.setChunked(true);
        req.dataHandler(new Handler<Buffer>() {
            public void handle(Buffer data) {
                //System.out.println("Proxying request body:" + data);
                cReq.write(data);
            }
        });
        req.endHandler(new VoidHandler() {
            public void handle() {
                //System.out.println("end of the request");
                cReq.end();
            }
        });
    }
    
    public void start() {
        // get the Config data
        config = container.config();

        // start the redis connection
        jedis = new Jedis(config.getString("redisHost"), config.getInteger("redisPort"));

        // start the server
        server = vertx.createHttpServer();
//                .setSSL(true)
//                .setKeyStorePath(config.getString("server.https.KeyStore"))
//                .setKeyStorePassword(config.getString("server.https.KeyStorePassword"))
//                .setClientAuthRequired(true);

        // start the websocket handler
        server.websocketHandler(new Handler<ServerWebSocket>() {
            public void handle(ServerWebSocket ws) {
                this.handle(ws);
            }
        }).listen(config.getInteger("server.port"))
                .listen(config.getInteger("server.https.port"));

        // start the HTTP handler
        server.requestHandler(new Handler<HttpServerRequest>() {
            public void handle(HttpServerRequest request) {
                this.handle(request);
            }
        }).listen(config.getInteger("server.port"))
                .listen(config.getInteger("server.https.port"));
        
    }
}
