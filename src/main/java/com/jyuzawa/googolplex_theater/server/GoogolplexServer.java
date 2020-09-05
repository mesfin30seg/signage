package com.jyuzawa.googolplex_theater.server;

import com.jyuzawa.googolplex_theater.client.GoogolplexController;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class runs the server for the web UI.
 *
 * @author jyuzawa
 */
public final class GoogolplexServer extends AbstractVerticle {

  private static final Logger LOG = LoggerFactory.getLogger(GoogolplexServer.class);

  public static final int DEFAULT_PORT = 8000;

  private final GoogolplexController controller;
  private final int port;

  public GoogolplexServer(GoogolplexController controller, int port) {
    this.controller = controller;
    this.port = port;
  }

  @Override
  public void start() throws Exception {
    Router router = Router.router(vertx);
    HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create(vertx);
    router
        .get("/")
        .handler(
            ctx -> {
              JsonObject data = new JsonObject();
              data.put("configuredDevices", controller.getConfiguredDevices());
              data.put("unconfiguredDevices", controller.getUnconfiguredDevices());
              engine.render(
                  data,
                  "templates/overview.hbs",
                  res -> {
                    if (res.succeeded()) {
                      ctx.response().end(res.result());
                    } else {
                      ctx.fail(res.cause());
                    }
                  });
            });
    router
        .post("/refresh")
        .handler(
            ctx -> {
              HttpServerRequest request = ctx.request();
              request
                  .setExpectMultipart(true)
                  .endHandler(
                      req -> {
                        String name = request.formAttributes().get("name");
                        controller.refresh(name);
                        JsonObject data = new JsonObject();
                        if (name == null) {
                          name = "All Devices";
                        }
                        data.put("name", name);
                        engine.render(
                            data,
                            "templates/refresh.hbs",
                            res -> {
                              if (res.succeeded()) {
                                ctx.response().end(res.result());
                              } else {
                                ctx.fail(res.cause());
                              }
                            });
                      });
            });
    router
        .get("/favicon.ico")
        .handler(
            ctx -> {
              ctx.response().putHeader("content-type", "image/png").sendFile("favicon.png");
            });
    // start a HTTP web server on port 8080
    vertx.createHttpServer().requestHandler(router).listen(port);
    LOG.info("Running server on port " + port);
  }
}
