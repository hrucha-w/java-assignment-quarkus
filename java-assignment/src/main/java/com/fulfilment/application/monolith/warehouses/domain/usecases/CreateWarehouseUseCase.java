package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  @Inject
  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    // Business Unit Code Verification
    Warehouse existingWarehouse = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existingWarehouse != null) {
      throw new WebApplicationException(
          "Warehouse with business unit code '" + warehouse.businessUnitCode + "' already exists.",
          422);
    }

    // Location Validation
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WebApplicationException(
          "Location '" + warehouse.location + "' is not valid.", 422);
    }

    // Warehouse Creation Feasibility - Check max number of warehouses
    WarehouseRepository repository = (WarehouseRepository) warehouseStore;
    List<Warehouse> activeWarehousesAtLocation = repository.findActiveByLocation(warehouse.location);
    if (activeWarehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WebApplicationException(
          "Maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + ") has already been reached for location '"
              + warehouse.location
              + "'.",
          422);
    }

    // Capacity and Stock Validation
    if (warehouse.capacity == null || warehouse.capacity <= 0) {
      throw new WebApplicationException("Warehouse capacity must be greater than 0.", 422);
    }

    if (warehouse.stock == null || warehouse.stock < 0) {
      throw new WebApplicationException("Warehouse stock cannot be negative.", 422);
    }

    if (warehouse.stock > warehouse.capacity) {
      throw new WebApplicationException(
          "Warehouse stock (" + warehouse.stock + ") cannot exceed capacity (" + warehouse.capacity + ").",
          422);
    }

    // Check total capacity at location
    int totalCapacityAtLocation =
        activeWarehousesAtLocation.stream()
            .mapToInt(w -> w.capacity != null ? w.capacity : 0)
            .sum();
    int newTotalCapacity = totalCapacityAtLocation + warehouse.capacity;
    if (newTotalCapacity > location.maxCapacity) {
      throw new WebApplicationException(
          "Total capacity at location '"
              + warehouse.location
              + "' would exceed maximum capacity ("
              + location.maxCapacity
              + "). Current total: "
              + totalCapacityAtLocation
              + ", New warehouse capacity: "
              + warehouse.capacity
              + ".",
          422);
    }

    // if all went well, create the warehouse
    warehouseStore.create(warehouse);
  }
}
