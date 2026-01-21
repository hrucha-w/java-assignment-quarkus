package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
public class WarehouseEndpointIT {

  private static final String PATH = "warehouse";

  @Test
  public void testListAllWarehouses() {
    // Test that the endpoint returns a list of warehouses
    // Note: Specific warehouse codes may vary due to test execution order
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("businessUnitCode"), containsString("location"));
  }

  @Test
  public void testGetWarehouseById() {
    given()
        .when()
        .get(PATH + "/1")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("ZWOLLE-001"));
  }

  @Test
  public void testGetWarehouseByBusinessUnitCode() {
    given()
        .when()
        .get(PATH + "/MWH.001")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("ZWOLLE-001"));
  }

  @Test
  public void testCreateWarehouseWithValidData() {
    String warehouseJson =
        "{"
            + "\"businessUnitCode\":\"MWH.999\","
            + "\"location\":\"AMSTERDAM-002\","
            + "\"capacity\":50,"
            + "\"stock\":10"
            + "}";

    // OpenAPI generated code returns 200, not 201
    given()
        .contentType("application/json")
        .body(warehouseJson)
        .when()
        .post(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.999"), containsString("AMSTERDAM-002"));
  }

  @Test
  public void testCreateWarehouseFailsWhenBusinessUnitCodeExists() {
    String warehouseJson =
        "{"
            + "\"businessUnitCode\":\"MWH.001\","
            + "\"location\":\"AMSTERDAM-001\","
            + "\"capacity\":50,"
            + "\"stock\":10"
            + "}";

    given()
        .contentType("application/json")
        .body(warehouseJson)
        .when()
        .post(PATH)
        .then()
        .statusCode(422)
        .body(containsString("already exists"));
  }

  @Test
  public void testCreateWarehouseFailsWhenLocationInvalid() {
    String warehouseJson =
        "{"
            + "\"businessUnitCode\":\"MWH.888\","
            + "\"location\":\"INVALID-LOCATION\","
            + "\"capacity\":50,"
            + "\"stock\":10"
            + "}";

    given()
        .contentType("application/json")
        .body(warehouseJson)
        .when()
        .post(PATH)
        .then()
        .statusCode(422)
        .body(containsString("not valid"));
  }

  @Test
  public void testArchiveWarehouse() {
    // Archive warehouse by ID
    given().when().delete(PATH + "/1").then().statusCode(204);

    // Verify it's no longer in the list
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(not(containsString("MWH.001")));
  }

  @Test
  public void testReplaceWarehouseWithMatchingStock() {
    String replacementJson =
        "{"
            + "\"businessUnitCode\":\"MWH.012\","
            + "\"location\":\"AMSTERDAM-001\","
            + "\"capacity\":75,"
            + "\"stock\":5"
            + "}";

    given()
        .contentType("application/json")
        .body(replacementJson)
        .when()
        .post(PATH + "/MWH.012/replacement")
        .then()
        .statusCode(200)
        .body(containsString("MWH.012"), containsString("75"));
  }

  @Test
  public void testReplaceWarehouseFailsWhenStockDoesNotMatch() {
    String replacementJson =
        "{"
            + "\"businessUnitCode\":\"MWH.012\","
            + "\"location\":\"AMSTERDAM-001\","
            + "\"capacity\":75,"
            + "\"stock\":999"
            + "}";

    given()
        .contentType("application/json")
        .body(replacementJson)
        .when()
        .post(PATH + "/MWH.012/replacement")
        .then()
        .statusCode(422)
        .body(containsString("must match"));
  }
}
