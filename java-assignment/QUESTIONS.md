# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, I would refactor to standardize on a consistent approach. Currently, there are three different patterns:

1. **Store** - Uses PanacheEntity (Active Record pattern): Entity has static methods like `Store.listAll()`, `Store.findById()`, and instance methods like `store.persist()`.

2. **Product** - Uses PanacheRepository (Repository pattern): Separate `ProductRepository` implements `PanacheRepository<Product>`, with the entity being a plain JPA entity.

3. **Warehouse** - Uses a hybrid approach with domain model separation: `DbWarehouse` entity (not PanacheEntity), `WarehouseRepository` implementing both `PanacheRepository<DbWarehouse>` and domain port `WarehouseStore`, with a separate domain model `Warehouse`.

**Recommendation: Standardize on the Repository pattern with domain model separation (Warehouse approach)**

Reasons:
- **Separation of Concerns**: Separating domain models from persistence entities (like Warehouse does) provides better architecture, allowing business logic to be independent of JPA/Hibernate annotations.
- **Testability**: Repository pattern is easier to mock and test in isolation compared to Active Record pattern.
- **Flexibility**: Domain models can evolve independently from database schema, making it easier to refactor or change persistence strategies.
- **Consistency**: Having one pattern across all entities reduces cognitive load and makes the codebase more maintainable.

**Refactoring Plan:**
- Convert `Store` from PanacheEntity to Repository pattern with a `StoreRepository`
- Keep `Product` as Repository pattern but consider adding domain model separation if business logic grows
- Keep `Warehouse` approach as the standard - it's the most mature and follows clean architecture principles

This refactoring would require updating `StoreResource` to inject `StoreRepository` instead of using static methods, but the benefits in terms of maintainability and testability outweigh the migration effort.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
**OpenAPI Code Generation Approach (Warehouse):**

Pros:
- **Contract-First Development**: API contract is defined upfront, reducing ambiguity between frontend/backend teams
- **Type Safety**: Generated code ensures compile-time type checking between API spec and implementation
- **Documentation**: OpenAPI spec serves as living documentation that stays in sync with code
- **Client Generation**: Can generate client SDKs for consumers automatically
- **Validation**: Generated code often includes built-in request/response validation
- **Consistency**: Enforces consistent API structure and naming conventions

Cons:
- **Build Complexity**: Requires code generation step in build process
- **Less Flexibility**: Changes require updating YAML first, then regenerating, which can slow iteration
- **Learning Curve**: Team needs to understand OpenAPI spec syntax and code generation tools
- **Generated Code**: Can be harder to debug and may generate verbose code
- **Tight Coupling**: Implementation must match generated interface exactly

**Manual Implementation Approach (Store/Product):**

Pros:
- **Simplicity**: Direct JAX-RS annotations are straightforward and easy to understand
- **Flexibility**: Easy to add custom logic, validations, or annotations without constraints
- **Fast Iteration**: No code generation step means faster development cycles
- **Full Control**: Complete control over endpoint implementation and structure
- **Less Tooling**: No need for OpenAPI generators or additional build configuration

Cons:
- **Documentation Drift**: API documentation can become outdated if not manually maintained
- **No Contract Enforcement**: No automatic validation that implementation matches intended API
- **Inconsistency Risk**: Different developers may implement endpoints differently
- **Manual Client Generation**: Frontend teams must manually create clients or use generic HTTP clients
- **No Type Safety**: Less compile-time safety between API contract and implementation

**My Choice: Hybrid Approach with OpenAPI for Public APIs**

I would use **OpenAPI code generation for Warehouse-style complex APIs** that:
- Have multiple consumers (internal/external)
- Require strict contracts and versioning
- Need comprehensive documentation
- Benefit from client SDK generation

I would use **manual implementation for simpler, internal APIs** like Store/Product when:
- APIs are straightforward CRUD operations
- Only internal consumers exist
- Rapid iteration is more important than strict contracts
- Team prefers simplicity over tooling overhead

**Recommendation for this codebase:**
- Keep Warehouse with OpenAPI (it's already set up and working)
- Consider migrating Store/Product to OpenAPI if they grow in complexity or gain external consumers
- Alternatively, create OpenAPI specs for Store/Product for documentation purposes without code generation, maintaining manual implementation for flexibility

The key is choosing the right tool for each API's requirements rather than forcing one approach everywhere.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
**Testing Strategy: Focus on High-Value Tests with Incremental Coverage**

Given time constraints, I would prioritize tests using the Testing Pyramid principle, focusing on areas with highest business value and risk.

**Priority 1: Integration Tests for Critical Business Logic (Warehouse Use Cases)**
- Focus on `CreateWarehouseUseCase`, `ReplaceWarehouseUseCase`, and `ArchiveWarehouseUseCase`
- These contain complex validation logic (capacity checks, location constraints, stock matching)
- Test all validation scenarios and edge cases
- Use in-memory database (H2) for fast execution
- **Why**: Business rules are the most critical and error-prone parts

**Priority 2: End-to-End API Tests for Core Workflows**
- Expand existing `WarehouseEndpointIT` and `ProductEndpointTest` style tests
- Test complete user workflows: create → retrieve → update → archive
- Use `@QuarkusIntegrationTest` for realistic environment testing
- **Why**: Catches integration issues between layers and validates API contracts

**Priority 3: Unit Tests for Complex Logic**
- Test validation logic in isolation (e.g., capacity calculations, location validation)
- Mock dependencies (repositories, gateways) for fast execution
- Focus on edge cases and error scenarios
- **Why**: Fast feedback loop for logic changes

**Priority 4: Repository Tests (Lower Priority)**
- Test custom query methods (e.g., `findActiveByLocation`, `findByBusinessUnitCode`)
- Use `@DataJpaTest` or in-memory database
- **Why**: Panache provides most functionality, custom queries need verification

**What to Skip Initially:**
- Extensive unit tests for simple CRUD operations (Store, Product)
- Tests for generated code (OpenAPI interfaces)
- Tests for framework functionality (Panache, JAX-RS)

**Maintaining Test Coverage Over Time:**

1. **Coverage Thresholds**: Set minimum coverage targets (e.g., 70% for business logic, 50% overall) using JaCoCo
2. **CI/CD Integration**: Fail builds if coverage drops below threshold
3. **Test with Code Reviews**: Require tests for new features in PR reviews
4. **Refactor Tests**: As code evolves, refactor tests to match new structure
5. **Focus on Quality**: Better to have fewer, well-written tests than many brittle ones
6. **Mutation Testing**: Periodically use tools like PIT to verify test quality, not just coverage numbers

**Implementation Approach:**
- Start with Warehouse use case tests (highest complexity)
- Add integration tests for critical paths
- Gradually expand to other areas as time permits
- Document test strategy so team understands priorities

**Example Test Structure:**
```
src/test/java/
  ├── integration/          # @QuarkusIntegrationTest - full stack tests
  │   └── WarehouseEndpointIT.java
  ├── usecases/             # Unit tests for business logic
  │   ├── CreateWarehouseUseCaseTest.java
  │   └── ReplaceWarehouseUseCaseTest.java
  └── repositories/         # Repository query tests
      └── WarehouseRepositoryTest.java
```

This approach balances thoroughness with pragmatism, ensuring critical business logic is well-tested while avoiding over-testing simple code.
```