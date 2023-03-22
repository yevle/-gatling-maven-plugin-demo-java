package demostore;

import java.time.Duration;
import java.util.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import io.gatling.recorder.internal.bouncycastle.asn1.cmp.Challenge;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class RecordedPostman extends Simulation {

  private HttpProtocolBuilder httpProtocol = http
    .baseUrl("https://demostore.gatling.io")
    .contentTypeHeader("application/json")
    .acceptHeader("application/json");

  private static final int USER_COUNT = Integer.parseInt(System.getProperty("USERS", "5"));
  private static final Duration RAMP_DURATION =
          Duration.ofSeconds(Integer.parseInt(System.getProperty("RAMP_DURATION", "20")));
  private static final Duration TEST_DURATION =
          Duration.ofSeconds(Integer.parseInt(System.getProperty("TEST_DURATION", "40")));

  @Override
  public void before() {
      System.out.printf("Running test:\n with %d users\n with %d ramp duration\n with %d test duration",
              USER_COUNT, RAMP_DURATION.getSeconds(), TEST_DURATION.getSeconds());
  }

  @Override
    public void after() {
        System.out.println("TEST FINISHED!");
    }

  private static ChainBuilder initSession = exec(session -> session.set("authenticated", false));

  private static class UserJorneys {

      private static Duration minPause = Duration.ofMillis(500);
      private static Duration maxPause = Duration.ofMillis(1500);

      public static ChainBuilder admin =
              exec(initSession)
              .exec(Category.list)
              .pause(minPause,maxPause)
              .exec(Product.list)
              .pause(minPause)
              .exec(Category.openById(7))
              .pause(maxPause)
              .exec(Product.getById(17))
              .pause(minPause,maxPause)
              .exec(Product.getRandom)
              .pause(minPause,maxPause)
              .exec(Product.update)
              .pause(minPause)
              .exec(Product.updateFromRaw)
              .pause(maxPause)
              .repeat(2,"Product Create").on(exec(Product.create))
              .pause(minPause)
              .exec(Product.create1)
              .exec(Category.update);

      public static ChainBuilder priceScrapper =
              exec(
                      Category.list,
                      pause(minPause, maxPause),
                      Product.listAll
              );

      public static  ChainBuilder priceUpdater =
              exec(initSession)
                      .exec(Product.listAll)
                      .pause(minPause, maxPause)
                      .repeat("#{allProds.size()}","productIndex").on(
                              exec(session -> {
                                  int index = session.getInt("productIndex");
                                  List<Object> allProducts = session.getList("allProds");
                                  return session.set("product", allProducts.get(index));
                              })
                      .exec(Product.update)
                      .pause(minPause, maxPause));
  }

  private static class Scenarios {

      public static ScenarioBuilder defaultScn = scenario("Default Load Test")
              .during(TEST_DURATION)
              .on(
                      randomSwitch().on(
                              Choice.withWeight(20d, UserJorneys.admin),
                              Choice.withWeight(40d, UserJorneys.priceScrapper),
                              Choice.withWeight(40d, UserJorneys.priceUpdater)
                      )
              );
      public static ScenarioBuilder noAdminScn = scenario("Without Admin Test")
              .during(30)
              .on(
                      randomSwitch().on(
                              Choice.withWeight(60d, UserJorneys.priceScrapper),
                              Choice.withWeight(40d, UserJorneys.priceUpdater)
                      )
              );

  }

  private ScenarioBuilder scn = scenario("RecordedPostman")
    .exec(initSession)
    .exec(Category.list)
    .pause(1)
    .exec(Product.list)
    .pause(1)
    .exec(Category.openById(7))
    .pause(1)
    .exec(Product.getById(17))
    .pause(1)
    .exec(Product.getRandom)
    .pause(1)
    .exec(Product.update)
    .pause(1)
    .exec(Product.updateFromRaw)
    .pause(1)
    .repeat(2,"Product Create").on(exec(Product.create))
    .pause(1)
    .exec(Product.create1)
    .exec(Category.update);

    {
        //Sequentially and parallel run
        setUp(
                Scenarios.defaultScn.
                        injectOpen(rampUsers(USER_COUNT).during(RAMP_DURATION)).protocols(httpProtocol),
//                        .andThen(
                        Scenarios.noAdminScn.injectOpen(rampUsers(5).during(20)).protocols(httpProtocol)
        );
    }

    //Regular (open) model simulation
//              scn.injectOpen(
//                      atOnceUsers(3),
//                      nothingFor(5),
//                      rampUsers(10).during(20),
//                      nothingFor(5),
//                      constantUsersPerSec(1).during(20)
//              )

              //Closed model simulation
//              scn.injectClosed(
//                      rampConcurrentUsers(1).to(5).during(20),
//                      constantConcurrentUsers(5).during(20)
//              )

              //Throttle simulation
//              Scenarios.defaultScn.injectOpen(constantUsersPerSec(2).during(80)).protocols(httpProtocol)
//                      .throttle(
//                              reachRps(10).in(20),
//                              holdFor(20),
//                              jumpToRps(20),
//                              holdFor(20)
//                      )).maxDuration(80);
}
