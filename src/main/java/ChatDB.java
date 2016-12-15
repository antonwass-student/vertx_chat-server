import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;


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
}
