package com.atwoki.hydro.messaging.gateway;

import com.atwoki.framework.postoffice.DefaultPostOfficeWorker;
import com.atwoki.framework.postoffice.DefaultWorkListener;
import com.atwoki.hydro.messaging.BrokerReceiver;
import com.atwoki.hydro.system.Helper;
import com.rabbitmq.client.*;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.ReplyException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: atwoki
 * Date: 2013/11/17
 * Time: 8:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class AMQPMessageReceiver implements BrokerReceiver {

    private EventBus _eb;
    private JsonObject _config;
    private MessageConsumer _consumer;
    private Logger _log;
    private boolean _configured;
    private ConnectionFactory _factory;
    private Connection _connection;

    /**
     *
     * @param c
     * @param v
     */
    @Override
    public void init(Container c, Vertx v) {
        _log = c.logger();
        _eb = v.eventBus();
        _factory = new ConnectionFactory();
    }

    /**
     *
     * @param config
     */
    @Override
    public void configure(JsonObject config) {
        if (!isConfigured()) {
            _config = config;
            _log.info("message consumer: loaded, cfg=" + _config);
            Channel c = createChannel();
            if (c != null) {
                try {
                    _consumer = new MessageConsumer(c, _config.getString("queue"));
                    _configured = true;
                } catch (Exception e) {
                    _log.error("amqp receiver: error, desc=" + e);
                }
            } else {
                _log.info("amqp recevier: channel failure, cfg=" + _config);
            }
        } else {
            _log.info("amqp recevier: not configured, cfg=" + _config);
        }
    }

    private Channel createChannel() {
        Channel c = null;
        try {
            if (_connection == null) {
                _factory.setUri(_config.getString("broker"));
                _connection = _factory.newConnection();
            }
            c = _connection.createChannel();
        } catch (Exception e) {
            _log.info("amqp recevier: connection failure, cfg=" + _config);
        }
        return c;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean isConfigured() {
        return _configured;
    }

    /**
     *
     */
    @Override
    public void receive() {
        if (isConfigured()) {
            try {
                _consumer.start();
            } catch (IOException e) {
                _log.error("amqp receiver: error starting consumer, desc=" + e);
            }
        }
    }

    /**
     *
     */
    @Override
    public void stop() {
        try {
            _connection.close();
            _connection = null;
            _factory = null;
            _configured = false;
        } catch (Exception e) {
            _log.error("amqp receiver: error stopping/cleaning up", e);
        }
    }

    protected class MessageConsumer extends DefaultConsumer {

        private MessageWorker __receiver;
        private String __queue;

        public MessageConsumer(Channel channel, String queue) {
            super(channel);
            __queue = queue;
            __receiver = new MessageWorker(Helper.getID());
            __receiver.addListener(new InboundWorkListener());
        }

        public void start() throws IOException {
            super.getChannel().basicConsume(__queue, _config.getBoolean("auto_acknowledge", false), this.getClass().getName(), this);
        }

        @Override
        public void handleDelivery(java.lang.String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                throws java.io.IOException {
            super.handleDelivery(consumerTag, envelope, properties, body);
            _log.info("amqp receiver: handle delivery, consumer=" + consumerTag);
            JsonObject message = new JsonObject()
                    .putBinary("data_bytes", body)
                    .putString("consumer_tag", consumerTag)
                    .putString("exchange", envelope.getExchange())
                    .putString("routing_key", envelope.getRoutingKey());
            __receiver.work(message);
        }
    }

    protected class MessageWorker extends DefaultPostOfficeWorker {
        public MessageWorker(String id) {
            super(id);
        }
        @Override
        public void work(Object message) {
            try {
                _log.info("amqp receiver: message worker [" + getId() + "] received work, notifying listeners");
                notifyListeners(message);
            } catch (Exception e) {
                _log.error("amqp receiver: message worker [" + getId() + "] error, desc=an error has occurred working a received message, msg=" + message, e);
            }
        }
    }

    protected class InboundWorkListener extends DefaultWorkListener {
        @Override
        public void notified(Object source, Object message) {
            int timeout = _config.getNumber("timeout", 1000).intValue();
            _log.info("amqp receiver: inbound listener invoked, source=" + source.getClass().getName() + ", sla=" + timeout);

            String address = _config.getString("address");
            JsonObject routes = _config.getObject("routes");
            if (routes != null) {
                address = routes.getString(((JsonObject) message).getString("routing_key"));
            }

            JsonObject data = new JsonObject(new Buffer(((JsonObject) message).getBinary("data_bytes")).toString());
            ((JsonObject) message).putObject("data", data);
            ((JsonObject) message).removeField("data_bytes");

            _log.info("amqp receiver: inbound listener dispatching, message=" + message + ", to=" + address);
            _eb.sendWithTimeout(address, (JsonObject) message, timeout, new Handler<AsyncResult<Message<JsonObject>>>() {
                @Override
                public void handle(AsyncResult<Message<JsonObject>> result) {
                    if (result.succeeded()) {
                        _log.info("amqp receiver: inbound listener did some work, result=" + result.result().body());
                    } else {
                        ReplyException e = (ReplyException) result.cause();
                        _log.info("amqp receiver: inbound listener dispatch failure, cause=" + e.failureType().name() + ", msg=" + e.getMessage());
                    }
                }
            });
        }
    }
}
