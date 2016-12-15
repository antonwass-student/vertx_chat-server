import com.microsoft.sqlserver.jdbc.SQLServerDriver;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

import java.sql.Connection;

/***
 * TODO: when a user connects to websocket, set a consumer for each conversation that the user is a member of.
 * TODO: client must specify which conversation he is sending the message to.
 * TODO: when a client creates a new conversation, add it to the consumer list
 *
 */
public class MyFirstVerticle extends AbstractVerticle {

    @Override
    public void start(){
        JsonObject sqlConfig = new JsonObject();
        sqlConfig.put("url", "jdbc:sqlserver://communitylab.database.windows.net:1433;database=community;");
        sqlConfig.put("driver_class", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        sqlConfig.put("user", "admincommunity");
        sqlConfig.put("password", "Antonchristian95");
        sqlConfig.put("min_pool_size", 3);
        JDBCClient sqlClient = JDBCClient.createShared(vertx, sqlConfig, "community.dbo");

        EventBus eb = vertx.eventBus();

        HttpServer server = vertx
                .createHttpServer()
                .websocketHandler(serverWebSocket -> {
            System.out.println("Connected!");

            //On socket close
            serverWebSocket.closeHandler(handler->{
                System.out.println("Disconnected!");
                eb.consumer("chat.message").unregister();
            });

            //Messages from the event bus
            eb.consumer("chat.conversation.1", message->{
                JsonObject msg = new JsonObject();
                msg.put("type", "message").put("message", message.body().toString());
                serverWebSocket.writeFinalTextFrame(msg.toString());
            });

            //Messages from client
            serverWebSocket.frameHandler(frame->{
                System.out.println(frame.textData());
                JsonObject json = new JsonObject(frame.textData());
                System.out.println(json.toString());

                switch(json.getString("type")){
                    case "message":
                        //send a message to a conversation
                        eb.publish("chat.conversation." + json.getString("id"), json.getString("message"));
                        //save message to database.
                        break;
                    case "conversations":
                        //do async sql call
                        ChatDB.getConversations(sqlClient, json.getInteger("user"), res->{
                            JsonObject message = new JsonObject();
                            message.put("type", "conversations");
                            message.put("conversations", new JsonArray(res.getRows()));
                            serverWebSocket.writeFinalTextFrame(message.toString());
                        });

                        break;
                    case "openConversation":
                        //open a conversation.
                        //register a consumer.
                        //get messages from it
                        break;
                    case "new":
                        //new conversation
                        break;
                    case "invite":
                        //add a user to conversation
                        break;
                    case "friends":
                        break;

                }
            });
        }).requestHandler(request -> {
            request.response().sendFile("web/index.html");
        }).listen(8090);
    }
}
