package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseRepository warehouseRepository;

  @Inject private CreateWarehouseOperation createWarehouseOperation;

  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;

  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;

  @Override
  public List<com.warehouse.api.beans.Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  @Transactional
  public com.warehouse.api.beans.Warehouse createANewWarehouseUnit(
      @NotNull com.warehouse.api.beans.Warehouse data) {
    Warehouse domainWarehouse = toDomainWarehouse(data);
    createWarehouseOperation.create(domainWarehouse);
    return toWarehouseResponse(domainWarehouse);
  }

  @Override
  public com.warehouse.api.beans.Warehouse getAWarehouseUnitByID(String id) {
    Warehouse warehouse = findWarehouseByIdOrBusinessUnitCode(id);
    if (warehouse == null) {
      throw new WebApplicationException(
          "Warehouse with id or business unit code '" + id + "' does not exist.", 404);
    }
    return toWarehouseResponse(warehouse);
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    Warehouse warehouse = findWarehouseByIdOrBusinessUnitCode(id);
    if (warehouse == null) {
      throw new WebApplicationException(
          "Warehouse with id or business unit code '" + id + "' does not exist.", 404);
    }
    archiveWarehouseOperation.archive(warehouse);
  }

  @Override
  @Transactional
  public com.warehouse.api.beans.Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull com.warehouse.api.beans.Warehouse data) {
    Warehouse domainWarehouse = toDomainWarehouse(data);
    // Ensure the business unit code matches
    domainWarehouse.businessUnitCode = businessUnitCode;
    replaceWarehouseOperation.replace(domainWarehouse);
    return toWarehouseResponse(domainWarehouse);
  }

  private Warehouse findWarehouseByIdOrBusinessUnitCode(String id) {
    // Try to find by database ID first (if id is numeric)
    try {
      Long dbId = Long.parseLong(id);
      DbWarehouse dbWarehouse = warehouseRepository.findById(dbId);
      if (dbWarehouse != null) {
        return dbWarehouse.toWarehouse();
      }
    } catch (NumberFormatException e) {
      // Not a numeric ID, try business unit code
    }
    // Try to find by business unit code
    return warehouseRepository.findByBusinessUnitCode(id);
  }

  private Warehouse toDomainWarehouse(com.warehouse.api.beans.Warehouse apiWarehouse) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = apiWarehouse.getBusinessUnitCode();
    warehouse.location = apiWarehouse.getLocation();
    warehouse.capacity = apiWarehouse.getCapacity();
    warehouse.stock = apiWarehouse.getStock();
    return warehouse;
  }

  private com.warehouse.api.beans.Warehouse toWarehouseResponse(Warehouse warehouse) {
    var response = new com.warehouse.api.beans.Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);
    return response;
  }
}
