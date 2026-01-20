package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  @Inject
  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    Warehouse existingWarehouse =
        warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existingWarehouse == null) {
      throw new WebApplicationException(
          "Warehouse with business unit code '"
              + newWarehouse.businessUnitCode
              + "' does not exist.",
          404);
    }

    if (existingWarehouse.archivedAt != null) {
      throw new WebApplicationException(
          "Cannot replace an archived warehouse with business unit code '"
              + newWarehouse.businessUnitCode
              + "'.",
          422);
    }

    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new WebApplicationException(
          "Location '" + newWarehouse.location + "' is not valid.", 422);
    }

    if (newWarehouse.capacity == null || newWarehouse.capacity <= 0) {
      throw new WebApplicationException("Warehouse capacity must be greater than 0.", 422);
    }

    if (newWarehouse.stock == null || newWarehouse.stock < 0) {
      throw new WebApplicationException("Warehouse stock cannot be negative.", 422);
    }

    if (!newWarehouse.stock.equals(existingWarehouse.stock)) {
      throw new WebApplicationException(
          "Stock of the new warehouse ("
              + newWarehouse.stock
              + ") must match the stock of the warehouse being replaced ("
              + existingWarehouse.stock
              + ").",
          422);
    }

    if (newWarehouse.capacity < newWarehouse.stock) {
      throw new WebApplicationException(
          "New warehouse capacity ("
              + newWarehouse.capacity
              + ") must be able to accommodate the stock ("
              + newWarehouse.stock
              + ").",
          422);
    }

    WarehouseRepository repository = (WarehouseRepository) warehouseStore;
    var activeWarehousesAtLocation = repository.findActiveByLocation(newWarehouse.location);
    int totalCapacityAtLocation =
        activeWarehousesAtLocation.stream()
            .filter(w -> !w.businessUnitCode.equals(existingWarehouse.businessUnitCode))
            .mapToInt(w -> w.capacity != null ? w.capacity : 0)
            .sum();
    int newTotalCapacity = totalCapacityAtLocation + newWarehouse.capacity;
    if (newTotalCapacity > location.maxCapacity) {
      throw new WebApplicationException(
          "Total capacity at location '"
              + newWarehouse.location
              + "' would exceed maximum capacity ("
              + location.maxCapacity
              + "). Current total (excluding replaced warehouse): "
              + totalCapacityAtLocation
              + ", New warehouse capacity: "
              + newWarehouse.capacity
              + ".",
          422);
    }

    warehouseStore.update(newWarehouse);
  }
}
