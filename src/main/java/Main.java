import io.vertx.core.Vertx;

/**
 * Created by Anton on 2016-12-14.
 */
public class Main {
    public static void main(String[]args){
        Vertx v = Vertx.vertx();

        v.deployVerticle(new MyFirstVerticle());


    }
}
