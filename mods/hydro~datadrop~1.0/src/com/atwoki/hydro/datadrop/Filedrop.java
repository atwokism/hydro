package com.atwoki.hydro.datadrop;

import com.atwoki.hydro.system.Helper;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: ezra
 * Date: 12/21/12
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class Filedrop extends BusModBase {

    private final static String DROP_URI_KEY = "drop_uri";
    private final static String DROP_SOURCE_KEY = "drop_src";
    private final static String DROP_SCAN_TIME_KEY = "scan_time";
    private final static String DROP_DISPATCH_ACTION_RESULT_KEY = "result";

    private String _dropUri;
    private long _scanTime = 1000, _scanTimer, _lastRun;

    public void start() {
        super.start();
        logger.info("starting File Drop\nconfig: " + Helper.jsonPrettyPrint(config));
        _scanTime = config.getLong(DROP_SCAN_TIME_KEY).longValue();
        _dropUri = config.getString(DROP_URI_KEY);
        _scanTimer = vertx.setPeriodic(_scanTime, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                _lastRun = System.currentTimeMillis();
                pingDrop(aLong);
            }
        });
    }

    public void stop() {
        vertx.cancelTimer(_scanTimer);
        try {
            super.stop();
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private void pingDrop(long pingId) {
        File dir = new File(_dropUri);
        if (dir.exists() && dir.isDirectory()) {
            File [] files = dir.listFiles();
            int c = 0;
            for (int n = 0; n < files.length; n++) {
                File file = files[n];
                if (file.isFile()) {
                    JsonObject jsonFileData = loadDataFile(file);
                    if (jsonFileData != null) {
                        jsonFileData.putString(DROP_URI_KEY, _dropUri);
                        jsonFileData.putString(DROP_SOURCE_KEY, file.getName());
                        dispatch(jsonFileData);
                        c++;
                    }
                }
            }
            if (c > 0) {
                long processDelta = System.currentTimeMillis() - _lastRun;
                logger.info("pinging: " + c + "/" + files.length + ", last swept " + processDelta + "s ago");
            }
        } else {
            logger.error("error: invalid drop [" + _dropUri + "] for ping id: " + pingId);
            logger.info("error: terminating ..");
            stop();
        }
    }

    private JsonObject loadDataFile(File file) {
        logger.info("loading: \"" + file.getName() + "\"");
        JsonObject jsonFileData = null;
        if (file != null && file.exists()) {
            BufferedReader reader = null;
            StringBuffer json = null;
            try {
                json = new StringBuffer();
                reader = new BufferedReader(new FileReader(file));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
                jsonFileData = new JsonObject(json.toString());
                reader.close();
                file.delete();
            } catch (Exception e) {
                logger.error("error: loadDataFile(" + file.getName() + ")", e);
            }
        } else {
            logger.info("error: invalid: " + file.getName());
        }
        return jsonFileData;
    }

    private void dispatch(final JsonObject jsonPackage) {
        logger.info("dispatching: " + jsonPackage);
        final String address = jsonPackage.getObject("meta").getString("address");
        eb.send(address, jsonPackage, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                logger.info("result: from " + jsonObjectMessage.replyAddress() + " for " + address);
                jsonPackage.putObject(DROP_DISPATCH_ACTION_RESULT_KEY, jsonObjectMessage.body());
                writeDataFile(jsonPackage);
            }
        });
        logger.info("sent: address=" + address + ", data=" + jsonPackage);
    }

    private void writeDataFile(JsonObject data) {
        logger.info("writing: " + data);
        String stamp = String.valueOf(System.currentTimeMillis());
        String tgtFileUri = data.getString(DROP_URI_KEY) + "/result/" + stamp + "_" + data.getString(DROP_SOURCE_KEY);
        File f = new File(tgtFileUri);
        try {
            FileWriter w = new FileWriter(f);
            w.write(data.toString());
            w.close();
        } catch (IOException e) {
            logger.error("error: could not write data file: " + tgtFileUri, e);
        }
    }
}

