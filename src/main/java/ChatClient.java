import io.vertx.core.eventbus.MessageConsumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Anton on 2016-12-15.
 */
public class ChatClient {
    private Map<Integer, MessageConsumer<String>> consumers =
            new HashMap<Integer, MessageConsumer<String>>();


    public boolean addConsumer(int key, MessageConsumer<String> consumer){
        if(!consumers.containsKey(key)){
            consumers.put(key, consumer);
            return true;
        }else{
            return false;
        }
    }

    public boolean isSubscribing(int key){
        return consumers.containsKey(key);
    }

    public void removeConsumer(int key){
        consumers.remove(key).unregister();
    }

    public void removeAll(){
        consumers.values().forEach(MessageConsumer::unregister);

        consumers.clear();
    }
}
