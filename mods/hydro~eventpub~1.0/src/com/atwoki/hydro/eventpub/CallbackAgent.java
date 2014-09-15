package com.atwoki.hydro.eventpub;

import com.atwoki.framework.factory.CallbackPublisher;
import com.atwoki.framework.factory.GenericFactory;
import com.atwoki.framework.postoffice.DefaultPostOfficeWorker;
import com.atwoki.framework.postoffice.DefaultWorkListener;
import com.atwoki.hydro.eventpub.postoffice.CallbackListener;
import com.atwoki.hydro.eventpub.postoffice.CallbackWorker;
import com.atwoki.hydro.system.Helper;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.*;
import za.co.mc.common.callback.GenericEventCallbackService;

/**
 * Created with IntelliJ IDEA.
 * User: ezra
 * Date: 1/1/13
 * Time: 2:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class CallbackAgent extends BusModBase {

    private GenericEventCallbackService _callbackService;
    private String _callbackAddress, _callbackRoute, _callbackHost, _callbackEndpoint, _callbackProxyEndpoint;
    private int _callbackPort, _callbackProxy;

    public void start() {
        super.start();
        logger.info("callback agent: starting, config=\n" + Helper.jsonPrettyPrint(config));

        _callbackAddress = getMandatoryStringConfig("address");
        _callbackRoute = getMandatoryStringConfig("route");
        _callbackHost = getMandatoryStringConfig("host");
        _callbackProxy = getMandatoryIntConfig("proxy");
        _callbackPort = getMandatoryIntConfig("port");

        _callbackEndpoint = "http://" + _callbackHost + ":" + _callbackPort + _callbackRoute;
        _callbackProxyEndpoint = "http://" + _callbackHost + ":" + _callbackProxy + _callbackRoute;

        _callbackService = new GenericEventCallbackService(createWorker());
        logger.info("callback agent: registered service, endpoint=" + _callbackEndpoint);

        startCallbackGateway();
    }

    private CallbackWorker createWorker() {
        CallbackWorker worker = new CallbackWorker(getVertx(), getContainer(), GenericFactory.makeUID());
        CallbackListener listener = new CallbackListener(getVertx(), getContainer());
        listener.setAddress(_callbackAddress);
        worker.addListener(listener);
        return worker;
    }

    private void startCallbackGateway() {
        CallbackPublisher.publishEndpoint(_callbackEndpoint, _callbackService);
        logger.info("callback agent: published service, endpoint=" + _callbackEndpoint);
        final HttpClient client = vertx.createHttpClient().setHost(_callbackHost).setPort(_callbackPort);
        HttpServer server = vertx.createHttpServer();
        RouteMatcher routeMatcher = new RouteMatcher();
        routeMatcher.all(_callbackRoute, new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest httpServerRequest) {
                final String cbEndpoint = "http://" + _callbackHost + ":" + _callbackPort + httpServerRequest.uri();
                logger.info("callback agent: proxy request, url=" + cbEndpoint);
                final Buffer __reqBuffer = new Buffer(0);
                final HttpClientRequest cReq
                        = client.request(httpServerRequest.method(), cbEndpoint, new Handler<HttpClientResponse>() {
                    final Buffer __resBuffer = new Buffer(0);
                    public void handle(HttpClientResponse cRes) {
                        logger.info("callback agent: proxy response, status=" + cRes.statusMessage() + ", code=" + cRes.statusCode());
                        httpServerRequest.response().setStatusCode(cRes.statusCode());
                        httpServerRequest.response().headers().clear().add(cRes.headers());
                        httpServerRequest.response().setChunked(true);
                        cRes.dataHandler(new Handler<Buffer>() {
                            public void handle(Buffer data) {
                                __resBuffer.appendBuffer(data);
                                httpServerRequest.response().write(data);
                            }
                        });
                        cRes.endHandler(new Handler() {
                            public void handle(Object o) {
                                auditBuffer("response", __resBuffer);
                                httpServerRequest.response().end();
                            }
                        });
                    }
                });
                cReq.headers().clear().add(httpServerRequest.headers());
                cReq.setChunked(true);
                httpServerRequest.dataHandler(new Handler<Buffer>() {
                    public void handle(Buffer data) {
                        __reqBuffer.appendBuffer(data);
                        cReq.write(data);
                    }
                });
                httpServerRequest.endHandler(new Handler() {
                    public void handle(Object o) {
                        auditBuffer("request", __reqBuffer);
                        cReq.end();
                    }
                });
            }
        });
        routeMatcher.noMatch(new Handler<HttpServerRequest>() {
            public void handle(HttpServerRequest req) {
                req.response().setStatusCode(404).end("Resource not found (404): No routes applicable.");
            }
        });
        server.requestHandler(routeMatcher).listen(_callbackProxy, _callbackHost);
        logger.info("callback agent: started proxy, endpoint=" + _callbackProxyEndpoint);
    }

    private void auditBuffer(String source, Buffer buffer) {
        logger.info("callback agent: audit buffer, src=" + source + ", address=" + _callbackAddress + ", bytes=" + buffer.length());
        eb.send(_callbackAddress + ".buffer", buffer);
    }

    public void stop() {
        try {
            super.stop();
        } catch (Exception e) {
            logger.error(e);
        }
    }
}
