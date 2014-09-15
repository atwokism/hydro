package com.atwoki.hydro.messaging.gateway;

import com.atwoki.hydro.messaging.BrokerSender;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

/**
 * Created with IntelliJ IDEA.
 * User: ezrak
 * Date: 2014/02/11
 * Time: 12:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class AMQPMessageSender implements BrokerSender {

    private Logger _log;
    private ConnectionFactory _factory;
    private Connection _connection;
    private String _trackerUri, _exchange, _routingKey;
    private boolean _configured;

    /**
     * Initializes the tracker factory.
     */
    public void init(Container c, Vertx v) {
        _log = c.logger();
        _configured = false;
    }

    /**
     *
     * @param config
     */
    @Override
    public void configure(JsonObject config) {
        configure(config.getString("broker"), config.getString("exchange"), config.getString("routing_key"));
    }

    /**
     * Configures the tracker factory with default parameters. This method should be called
     * before using the <code>track(buffer)</code> method.
     *
     * @param configurationUri The AMQP URI of the remote messaging host
     * @param exchange The AMQP exchange to which to publish messages to
     * @param routingKey The message routing-key
     */
    protected void configure(String configurationUri, String exchange, String routingKey) {
        if (isConfigured()) cleanup(); // if already configured cleanup and reset
        try {
            _trackerUri = configurationUri;
            _exchange = exchange;
            _routingKey = routingKey;
            _factory = new ConnectionFactory();
            _factory.setUri(_trackerUri);
            _connection = _factory.newConnection();
            _configured = true;
            _log.info("amqp sender: configured, uri=" + _trackerUri);
        } catch (Exception e) {
            e.printStackTrace();
            _configured = false;
            _log.error("amqp sender: error, could not configure tracker, desc=" + e + ", uri=" + _trackerUri);
        }
    }

    /**
     * Check to see if the tracker factory has been configured.
     *
     * @return The value <code>true</code> if the tracker factory is redy for use, <code>false</code>
     * otherwise.
     */
    public boolean isConfigured() {
        return _configured;
    }

    /**
     *
     * @param message
     */
    @Override
    public void send(StringBuffer message) {
        try {
            send((Object) message);
        } catch (Exception e) {
            e.printStackTrace();
            _log.error(e);
        }
    }

    /**
     * Provides a URI based string representation of the tracker factory's default configuration.
     *
     * @return A <code>String</code> value representing the tracker factory's default configuration.
     */
    public String configuration() {
        return _trackerUri + "?exchange=" + _exchange + "&routingkey=" + _routingKey;
    }

    /**
     * Sends a message to the remote AMQP host using the default values for exchange and routing-key
     * as provided in the <code>configure(string, string, string)</code> method.
     *
     * @param buffer The message data to be sent as a buffer. Current implementation only supports <code>StringBuffer</code>
     */
    protected void send(Object buffer) throws Exception {
        send(buffer, _exchange, _routingKey);
    }

    /**
     * Sends a message to the remote AMQP host using method supplied values for exchange and routing-key
     *
     * @param buffer The message data to be sent as a buffer. Current implementation only supports <code>StringBuffer</code>
     * @param exchange The AMQP exchange to which to publish messages to
     * @param routingKey The message routing-key
     */
    protected void send(Object buffer, String exchange, String routingKey) throws Exception {
        if (buffer != null && exchange != null && routingKey != null) {
            Channel c = _connection.createChannel();
            c.basicPublish(exchange, routingKey, null, buffer.toString().getBytes());
            _log.info("amqp sender: sent message, exchange=" + exchange + ", routingKey=" + routingKey + ", msg=" + buffer);
            c.close();
        }
    }

    /**
     * Resets the tracker factory and removes the default configuration. The tracker factory  will not be usable
     * subsequent to this call.
     */
    public void cleanup() {
        try {
            _connection.close();
            _connection = null;
            _factory = null;
            _configured = false;
        } catch (Exception e) {
            _log.error("amqp sender: error cleaning up, desc=" + e);
        }
    }
}
