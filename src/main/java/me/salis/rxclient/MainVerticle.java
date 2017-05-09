package me.salis.rxclient;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;

public class MainVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    private static final int SERVER_PORT = 9870;
    private static final String EB_PATH = "/eventbus/*";

    // addresses
    private static final String PUBLISHER_ADDRESS = "publisher";
    private static final String ECHO_ADDRESS = "echo";
    private static final String FAIL_ADDRESS = "fail";
    private static final String PING_PONG_ADDRESS = "ping-pong";

    private static final String RELAY_ADDRESS_PREFIX = "relay.";

    public void start() throws Exception {
        Router router = Router.router(vertx);
        // setup cors
        router.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.POST).allowedHeader("content-type"));

        // setup bridge
        BridgeOptions bridgeOptions = new BridgeOptions()
                // IN
                .addInboundPermitted(new PermittedOptions().setAddress(ECHO_ADDRESS))
                .addInboundPermitted(new PermittedOptions().setAddress(FAIL_ADDRESS))
                .addInboundPermitted(new PermittedOptions().setAddress(PING_PONG_ADDRESS))
                .addInboundPermitted(new PermittedOptions().setAddressRegex(RELAY_ADDRESS_PREFIX))
                // OUT
                .addOutboundPermitted(new PermittedOptions().setAddress(PUBLISHER_ADDRESS))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex(RELAY_ADDRESS_PREFIX));
        SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(bridgeOptions, event -> {
            logger.info(event);
            event.complete(true);
        });
        router.route(EB_PATH).handler(ebHandler);
        vertx.createHttpServer().requestHandler(router::accept).listen(SERVER_PORT);

        EventBus eb = vertx.eventBus();

        // Publisher
        vertx.setPeriodic(100, v -> {
            eb.publish(PUBLISHER_ADDRESS, "PUBLISHER_MESSAGE");
        });

        // Echo
        eb.consumer(ECHO_ADDRESS)
                .<JsonObject>toObservable()
                .subscribe((Message message) -> {
                    message.reply(message.body());
                }, this::errorHandler);
        eb.consumer(FAIL_ADDRESS)
                .toObservable()
                .subscribe((Message message) -> {
                    JsonObject body = JsonObject.mapFrom(message.body());
                    message.fail(body.getInteger("failureCode", 1), body.getString("message", "default message"));
                }, this::errorHandler);
        eb.consumer(PING_PONG_ADDRESS)
                .toObservable()
                .subscribe(this::pingPongHandler, this::errorHandler);
    }

    private void pingPongHandler(Message<Object> message) {
        if (!"ping".equals(message.body())) {
            return;
        }
        message.rxReply("pong").subscribe(this::pingPongHandler);
    }

    private void errorHandler(Throwable e) {
        logger.error(e);
    }
}
