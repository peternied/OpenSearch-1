import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.common.xcontent.XContentType;

public class TestCreationDate {
    public static void main(String[] args) {
        // Test 1: Basic creation date setting
        CreateIndexRequest request = new CreateIndexRequest("test-index");
        long testDate = 1640995200000L; // January 1, 2022 00:00:00 UTC
        request.creationDate(testDate);
        
        System.out.println("Creation date set: " + request.creationDate());
        System.out.println("Expected: " + testDate);
        System.out.println("Match: " + (request.creationDate().equals(testDate)));
        
        // Test 2: JSON parsing with creation_date
        String jsonWithCreationDate = "{\n" +
            "  \"settings\": {\n" +
            "    \"number_of_shards\": 1\n" +
            "  },\n" +
            "  \"creation_date\": 1640995200000\n" +
            "}";
        
        CreateIndexRequest request2 = new CreateIndexRequest("test-index-2");
        try {
            request2.source(jsonWithCreationDate, XContentType.JSON);
            System.out.println("JSON parsing successful");
            System.out.println("Parsed creation date: " + request2.creationDate());
        } catch (Exception e) {
            System.out.println("JSON parsing failed: " + e.getMessage());
        }
    }
}