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
                    System.out.println("A result is here!");
                    if(res.succeeded()){
                        System.out.println("Something succeeded");
                        handler.handle(res.result());
                    }else{
                        //failed
                        System.out.println("It failed...");
                        res.cause().printStackTrace();
                    }

                    conn.close();
                });
            }else{
                System.out.println("Could not get a connection to database.");
            }
        });
    }
}
