package demostore;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

public class Category {

    public static FeederBuilder.Batchable<String> catFeeder =
            csv("data/cat_feeder.csv").random();

    public static ChainBuilder list =
            exec((http("List Categories")
                    .get("/api/category")
                    .check(jmesPath("([?id==`6`])[0].name").is("For Her"))
            ));

    public static ChainBuilder update =
            feed(catFeeder)
                    .exec(Authenticate.authenticate)
                    .exec(http("Update Category")
                            .put("/api/category/#{catId}")
                            .headers(Headers.authorization)
                            .body(StringBody("{\"name\": \"#{catName}\"}"))
                            .check(jsonPath("$.name").isEL("#{catName}"))
                    );

    public static ChainBuilder openById (int id) {
        return exec(http(String.format("Open category id %s",id))
                .get(String.format("/api/category/%s",id))
        );
    }
}
