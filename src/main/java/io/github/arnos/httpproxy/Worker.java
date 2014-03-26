package io.github.arnos.httpproxy;
/*
 * Copyright 2014, Arno Schulz
 *
 * This file is licensed under the MIT License (MIT) (see License file)
 *
 * @author <a href="https://arnos.github.io">Arno Schulz</a>
 */

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;
import redis.clients.jedis.Jedis;

public class Worker extends Verticle {

    public void start() {

        Jedis jedis = new Jedis("localhost");
        jedis.set("foo", "bar");
        String value = jedis.get("foo");

        vertx.eventBus().registerHandler("ping-address", new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                message.reply("pong!");
                container.logger().info("Sent back pong");
            }
        });

        container.logger().info("PingVerticle started");

    }
}
