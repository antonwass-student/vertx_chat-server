import com.microsoft.sqlserver.jdbc.SQLServerDriver;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/***
 * TODO: when a user connects to websocket, set a consumer for each conversation that the user is a member of.
 * TODO: client must specify which conversation he is sending the message to.
 * TODO: when a client creates a new conversation, add it to the consumer list
 *
 */
public class MyFirstVerticle extends AbstractVerticle {

    private Map<String,ChatClient> clients = new HashMap<String, ChatClient>();

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

        HttpServer server = vertx.createHttpServer().websocketHandler(serverWebSocket -> {

            clients.put(serverWebSocket.textHandlerID(), new ChatClient());

            serverWebSocket.closeHandler(handler ->{
                clients.remove(serverWebSocket.textHandlerID()).removeAll();
            });

            //Messages from client
            serverWebSocket.frameHandler(frame->{
                System.out.println(frame.textData());
                JsonObject json = new JsonObject(frame.textData());

                switch(json.getString("type")){
                    case "message":
                        //send a message to a conversation
                        eb.publish("chat.conversation." + json.getString("id"), json.getString("message"));
                        //save message to database.

                        ChatDB.saveMessageToDB(sqlClient,
                                Integer.parseInt(json.getString("id")),
                                Integer.parseInt(json.getString("sender")),
                                json.getString("message"));
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
                        //maybe a init function that creates consumers for each conversation.
                        //register a consumer for the specified conversation
                        int id = Integer.parseInt(json.getString("id"));
                        ChatClient cc = clients.get(serverWebSocket.textHandlerID());
                        if(!cc.isSubscribing(id)){

                            MessageConsumer<String> consumer = eb.consumer("chat.conversation." + json.getString("id"));
                            cc.addConsumer(id, consumer);

                            consumer.handler( message->{
                                JsonObject msg = new JsonObject();
                                msg.put("type", "message").put("message", message.body().toString());
                                serverWebSocket.writeFinalTextFrame(msg.toString());
                            });
                        }

                        //get messages of conversation
                        ChatDB.getMessagesFromConversation(sqlClient, Integer.parseInt(json.getString("id")), res->{
                            JsonObject message = new JsonObject();
                            message.put("type", "messages");
                            message.put("messages", new JsonArray(res.getRows()));
                            serverWebSocket.writeFinalTextFrame(message.toString());
                        });

                        break;
                    case "new":
                        //new conversation
                        ChatDB.newConversation(sqlClient, json.getString("name"), json.getJsonArray("members"));
                        break;
                    case "invite":
                        ChatDB.addMembersToConversation(sqlClient, json.getJsonArray("members"), json.getInteger("id"));
                        break;
                    case "friends":
                        ChatDB.getFriendsOfUser(sqlClient, Integer.parseInt(json.getString("id")), res->{
                            JsonObject msg = new JsonObject();
                            msg.put("type", "friends");
                            msg.put("friends", new JsonArray(res));
                            serverWebSocket.writeFinalTextFrame(msg.toString());
                        });
                        break;
                    case "subscribe":

                        break;

                }
            });
        }).requestHandler(request -> {
            request.response().sendFile("web/index.html");
        }).listen(8090);
    }
}
