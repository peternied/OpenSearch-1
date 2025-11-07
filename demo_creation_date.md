# OpenSearch Index Creation Date Feature

## Summary
This update allows users to supply a custom creation date when creating an index in OpenSearch.

## Implementation Details

### 1. Request Classes Updated
- **CreateIndexRequest.java**: Added `creationDate` field with validation
  - Validates that creation date is not negative
  - Validates that creation date is not in the future
  
- **CreateIndexClusterStateUpdateRequest.java**: Already has `creationDate` field

### 2. Transport Layer
- **TransportCreateIndexAction.java**: Already passes creation date from request to cluster state update

### 3. Metadata Service
- **MetadataCreateIndexService.java**: Uses user-supplied creation date if provided, otherwise defaults to current time

### 4. Key Code Changes

#### CreateIndexRequest.java - Added validation:
```java
public CreateIndexRequest creationDate(long creationDate) {
    if (creationDate < 0) {
        throw new IllegalArgumentException("creation date must be non-negative, got: " + creationDate);
    }
    if (creationDate > System.currentTimeMillis()) {
        throw new IllegalArgumentException("creation date cannot be in the future, got: " + creationDate);
    }
    this.creationDate = creationDate;
    return this;
}
```

#### MetadataCreateIndexService.java - Already implemented logic:
```java
if (indexSettingsBuilder.get(SETTING_CREATION_DATE) == null) {
    Long creationDate = request.creationDate();
    if (creationDate != null) {
        indexSettingsBuilder.put(SETTING_CREATION_DATE, creationDate);
    } else {
        indexSettingsBuilder.put(SETTING_CREATION_DATE, Instant.now().toEpochMilli());
    }
}
```

### 5. Usage Examples

#### Via REST API (JSON):
```json
PUT /my-index
{
  "creation_date": 1640995200000,
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  }
}
```

#### Via Java API:
```java
CreateIndexRequest request = new CreateIndexRequest("my-index");
request.creationDate(1640995200000L); // January 1, 2022 00:00:00 UTC
client.admin().indices().create(request);
```

### 6. Test Coverage
Added comprehensive tests in `CreateIndexRequestTests.java`:
- Test setting valid creation date
- Test parsing creation date from JSON
- Test validation for negative dates (throws IllegalArgumentException)
- Test validation for future dates (throws IllegalArgumentException)
- Test invalid type for creation_date field (throws OpenSearchParseException)

### 7. Backward Compatibility
- The feature is fully backward compatible
- If no creation date is provided, the system defaults to the current time (existing behavior)
- Serialization/deserialization handles version compatibility (only available in OpenSearch 3.0.0+)

## Benefits
1. **Migration Support**: When migrating indices from other systems, you can preserve the original creation date
2. **Testing**: Allows creating indices with specific dates for testing scenarios
3. **Audit Compliance**: Helps maintain accurate historical records when recreating indices
4. **Backup/Restore**: Preserves original creation dates when restoring from backups

## Validation Rules
1. Creation date must be non-negative (>= 0)
2. Creation date cannot be in the future
3. Creation date must be a valid long value representing milliseconds since epoch

## File Changes
- `server/src/main/java/org/opensearch/action/admin/indices/create/CreateIndexRequest.java` - Added validation
- `server/src/test/java/org/opensearch/action/admin/indices/create/CreateIndexRequestTests.java` - Added test cases

All other necessary components were already in place to support this feature.
