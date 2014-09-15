package com.atwoki.hydro.auth;

import org.vertx.java.core.json.JsonObject;

import javax.swing.text.DateFormatter;
import java.text.DateFormat;
import java.util.Calendar;

/**
 * Created with IntelliJ IDEA.
 * User: atwoki
 * Date: 2014/02/14
 * Time: 12:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class Helper {

    public static JsonObject authTicket(LoginProfile profile, String action) {
        // TODO - improve intelligence to handle login logout logout event auth structure separately
        return new JsonObject()
                .putString("action", action)
                .putString("session", profile.session())
                .putString("secret", profile.secret())
                .putString("client", profile.client())
                .putString("hash", profile.hash())
                .putString("principal", profile.principal())
                .putNumber("validity", profile.validity())
                .putString("expiry", profile.expiry())
                .putString("sender", profile.sender());
    }

    public static String expiryTime(int validity) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, validity);
        return c.getTime().toString();
    }
}
