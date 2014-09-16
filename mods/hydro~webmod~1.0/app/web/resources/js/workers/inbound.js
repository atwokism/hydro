/**
 * Created with JetBrains WebStorm.
 * User: atwoki
 * Date: 2013/10/22
 * Time: 8:16 PM
 * To change this template use File | Settings | File Templates.
 */
importScripts('/resources/js/lib/hydro-0.0.1.js');

self.onmessage = function(envelope) {

    self.postMessage({
        "msg_id": envelope.data.message.response_id,
        "status": 'queued',
        "msg_txt": envelope.data.message.response_message,
        "route": envelope.data.source
    });
}
