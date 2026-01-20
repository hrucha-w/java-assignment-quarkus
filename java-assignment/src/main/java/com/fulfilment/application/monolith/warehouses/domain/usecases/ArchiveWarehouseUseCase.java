package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;

  @Inject
  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void archive(Warehouse warehouse) {
    Warehouse existingWarehouse =
        warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existingWarehouse == null) {
      throw new WebApplicationException(
          "Warehouse with business unit code '"
              + warehouse.businessUnitCode
              + "' does not exist.",
          404);
    }

    if (existingWarehouse.archivedAt != null) {
      throw new WebApplicationException(
          "Warehouse with business unit code '"
              + warehouse.businessUnitCode
              + "' is already archived.",
          422);
    }

    warehouse.archivedAt = LocalDateTime.now();
    warehouse.location = existingWarehouse.location;
    warehouse.capacity = existingWarehouse.capacity;
    warehouse.stock = existingWarehouse.stock;
    warehouse.createdAt = existingWarehouse.createdAt;

    warehouseStore.update(warehouse);
  }
}
