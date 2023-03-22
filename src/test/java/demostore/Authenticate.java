package demostore;

import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.http.HttpDsl.http;

public class Authenticate {
    public static ChainBuilder authenticate =
            doIf(session -> !session.getBoolean("authenticated")).then(
                    exec(http("Authenticate")
                            .post("/api/authenticate")
                            .body(RawFileBody("computerdatabase/recordedpostman/auth.json"))
                            .check(jsonPath("$.token").saveAs("jwt")))
                            .exec(session -> session.set("authenticated", true))
            );

}
