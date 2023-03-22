package demostore;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

public class Product {
    public static FeederBuilder.Batchable<String> prodFeeder =
            csv("data/prod_feeder.csv").circular();

    public static ChainBuilder list =
            exec((http("List Products")
                    .get("/api/product?category=7")
                    .check(jsonPath("$[?(@.categoryId!='7')]").notExists())
                    .check(jmesPath("[*].id").ofList().saveAs("allProdIds"))
            ));

    public static ChainBuilder listAll =
            exec((http("List All Products")
                    .get("/api/product")
                    .check(jmesPath("[*]").ofList().saveAs("allProds"))
            ));
    public static ChainBuilder updateFromRaw =
            exec(Authenticate.authenticate)
                    .exec(http("Update Product")
                            .put("/api/product/34")
                            .headers(Headers.authorization)
                            .body(RawFileBody("computerdatabase/recordedpostman/edit_p.json"))
                            .check(jsonPath("$.name").is("My new product"))
                    );

    public static ChainBuilder update =
            exec(Authenticate.authenticate)
                    .exec(session -> {
                        Map<String,Object> product = session.getMap("product");
                        return session
                                .set("prodName", product.get("name"))
                                .set("prodDescription", product.get("description"))
                                .set("prodPrice", product.get("price"))
                                .set("prodCategoryId", product.get("categoryId"))
                                .set("prodImage", product.get("image"))
                                .set("prodId", product.get("id"));
                    })
                    .exec(http("Update Product #{prodName}")
                            .put("/api/product/#{prodId}")
                            .headers(Headers.authorization)
                            .body(ElFileBody("computerdatabase/recordedpostman/create.json"))
                            .check(jsonPath("$.name").isEL("#{prodName}"))
                    );

    public static ChainBuilder getRandom =
            exec(session -> {
                List<Integer> allProds = session.getList("allProdIds");
                return session.set("prodId", allProds.get(new Random().nextInt(allProds.size())));
            })
                    .exec(http("Open product id #{prodId}")
                            .get("/api/product/#{prodId}")
                            .check(jsonPath("$.id").ofString().isEL("#{prodId}"))
                            .check(jmesPath("@").ofMap().saveAs("product")));

    public static ChainBuilder getById(int id) {
        return exec(http(String.format("Open product id %s",id))
                .get(String.format("/api/product/%s",id))
                .check(jsonPath("$.id").ofInt().is(id)));
    }

    public static ChainBuilder create =
            exec(Authenticate.authenticate)
                    .feed(prodFeeder)
                    .exec(
                            http("Create Product #{prodDescription}")
                                    .post("/api/product")
                                    .headers(Headers.authorization)
                                    .body(ElFileBody("computerdatabase/recordedpostman/create.json"))
                    )
                    .pause(1);

    public static ChainBuilder create1 =
            exec(Authenticate.authenticate)
                    .repeat(2, "prodCount").on(
                            exec(
                                    http("Create Product #{prodCount}")
                                            .post("/api/product")
                                            .headers(Headers.authorization)
                                            .body(RawFileBody("computerdatabase/recordedpostman/create#{prodCount}.json"))
                            )
                                    .pause(1)
                    );
}
