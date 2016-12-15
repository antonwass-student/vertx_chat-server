import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.List;

import static java.awt.SystemColor.text;


/**
 * Created by Anton on 2016-12-14.
 */
public class ChatDB {

    public static void getConversations(JDBCClient client, int userId, Handler<ResultSet> handler){
        client.getConnection(connResult->{
            if(connResult.succeeded()){
                SQLConnection conn = connResult.result();
                String query =
                        " SELECT conversation_id, namn" +
                        " FROM User_Conversation" +
                        " INNER JOIN Conversation" +
                        " ON User_Conversation.convo_id = Conversation.conversation_id " +
                        " INNER JOIN Usr" +
                        " ON User_Conversation.user_id = Usr.user_id" +
                        " WHERE User_Conversation.user_id = ?";

                JsonArray params = new JsonArray().add(userId);

                conn.queryWithParams(query, params, res->{
                    if(res.succeeded())
                        handler.handle(res.result());
                    else
                        res.cause().printStackTrace();

                    conn.close();
                });
            }else{
                connResult.cause().printStackTrace();
            }
        });
    }

    public static void getMessagesFromConversation(JDBCClient client, int convId, Handler<ResultSet> handler){

        client.getConnection(connResult->{
            if(connResult.succeeded()){
                SQLConnection conn = connResult.result();

                String query = "SELECT * FROM Message WHERE receiver = ?";

                JsonArray params = new JsonArray().add(convId);

                conn.queryWithParams(query, params, res->{
                    if(res.succeeded()){
                        handler.handle(res.result());
                    }else{
                        res.cause().printStackTrace();
                    }

                    conn.close();
                });
            }else{
                connResult.cause().printStackTrace();
            }
        });
    }

    public static void saveMessageToDB(JDBCClient client, int receiver, int sender, String text){

        client.getConnection(connResult->{
            if(connResult.succeeded()){
                SQLConnection conn = connResult.result();

                String query = "INSERT INTO Message (sender, receiver, text) VALUES(?,?,?)";

                JsonArray params = new JsonArray().add(sender).add(receiver).add(text);

                conn.updateWithParams(query, params, res->{
                    conn.close();
                });
            }else{
                connResult.cause().printStackTrace();
            }
        });
    }

    public static void newConversation(JDBCClient client,
                                       String groupName,
                                       JsonArray members){

        client.getConnection(connResult->{
            if(connResult.succeeded()){
                SQLConnection conn = connResult.result();


                String query = "INSERT INTO Conversation (namn) VALUES(?)";

                JsonArray params = new JsonArray().add(groupName);
                conn.updateWithParams(query, params, res->{
                    if(res.succeeded()){
                        String queryScope = "SELECT TOP 1 conversation_id FROM Conversation ORDER BY conversation_id DESC;";

                        conn.query(queryScope, resId->{
                            int id = resId.result().getResults().get(0).getInteger(0);
                            addMembersToConversation(client, members, id);
                        });


                    }
                    else{
                        res.cause().printStackTrace();
                    }

                    conn.close();
                });
            }else{
                connResult.cause().printStackTrace();
            }
        });
    }

    public static void addMembersToConversation(JDBCClient client, JsonArray members, int convId){
        client.getConnection(connResult->{
            if(connResult.succeeded()){
                SQLConnection conn = connResult.result();

                //language=TSQL
                String query = "INSERT INTO User_Conversation (user_id, convo_id) VALUES(?,?)";

                List<JsonArray>paramList = new ArrayList<JsonArray>();
                for(int i = 0; i < members.size(); i++){
                    paramList.add(new JsonArray().add(members.getInteger(i)).add(convId));
                }

                conn.batchWithParams(query, paramList, res->{
                    conn.close();
                });
            }else{
                connResult.cause().printStackTrace();
            }
        });
    }

    public static void getFriendsOfUser(JDBCClient client, int userId, Handler<List<JsonObject>> handler){
        client.getConnection(connResult->{
            if(connResult.succeeded()){
                SQLConnection conn = connResult.result();

                //language=TSQL
                String query = "SELECT u1.user_id, u1.name FROM Friendship "+
                        "INNER JOIN Usr as u1 ON Friendship.receiver = u1.user_id "+
                        "INNER JOIN Usr as u2 ON Friendship.inviter = u2.user_id "+
                        "WHERE u2.user_id = ?";

                String query2 = "SELECT u2.user_id, u2.name FROM Friendship "+
                "INNER JOIN Usr as u1 ON Friendship.receiver = u1.user_id "+
                "INNER JOIN Usr as u2 ON Friendship.inviter = u2.user_id "+
                "WHERE u1.user_id = ?";

                JsonArray params = new JsonArray().add(userId);

                conn.queryWithParams(query, params, res->{
                    if(res.succeeded()){
                        conn.queryWithParams(query2, params, res2->{
                            List<JsonObject> friends = new ArrayList<JsonObject>();
                            friends.addAll(res.result().getRows());
                            friends.addAll(res2.result().getRows());
                            handler.handle(friends);
                        });

                    }else{
                        res.cause().printStackTrace();
                    }

                    conn.close();
                });
            }else{
                connResult.cause().printStackTrace();
            }
        });
    }


}
