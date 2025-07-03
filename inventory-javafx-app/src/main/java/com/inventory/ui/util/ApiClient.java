package com.inventory.ui.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.inventory.model.Product;
import com.inventory.model.Receipt;
import com.inventory.model.Sale; // Assuming Sale is also a POJO in frontend.model
import com.inventory.model.User;
import com.inventory.model.StockAdjustment; // Import StockAdjustment POJO
import com.inventory.model.Category; // Import Category POJO


import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set; // For user roles as Set<String>
import java.util.stream.Collectors;

/**
 * Client for interacting with the Spring Boot backend API.
 * Handles HTTP requests, JSON serialization/deserialization, and JWT authentication.
 */
public class ApiClient {

    private static final String BASE_URL = "http://localhost:8080/api";
    private final HttpClient httpClient;
    private final Gson gson;
    private String jwtToken; // Stores the JWT token after successful login
    private List<String> loggedInUserRoles; // Stores roles of the logged-in user

    public ApiClient() {
        this.httpClient = HttpClient.newBuilder().build();
        // Configure Gson to handle LocalDateTime correctly
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    /**
     * Sets the JWT token received after successful authentication.
     * This token will be used in subsequent requests for authorization.
     * @param jwtToken The JWT token string.
     */
    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
        System.out.println("🔐 JWT token set in ApiClient.");
    }

    /**
     * Sets the roles of the currently logged-in user.
     * @param roles A List of String representing the user's roles (e.g., "OWNER", "CASHIER").
     */
    public void setLoggedInUserRoles(List<String> roles) {
        // IMPORTANT FIX: Strip "ROLE_" prefix from roles received from backend
        // This was already present in the previous version, ensuring it's still here.
        this.loggedInUserRoles = roles.stream()
                .map(role -> role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role)
                .collect(Collectors.toList());
        System.out.println("🎭 Roles set: " + this.loggedInUserRoles);
    }

    /**
     * Retrieves the roles of the currently logged-in user.
     * @return A List of String representing the user's roles, or an empty list if not set.
     */
    public List<String> getLoggedInUserRoles() {
        return loggedInUserRoles != null ? loggedInUserRoles : List.of();
    }

    /**
     * Helper method to build an HttpRequest with the JWT token in the Authorization header.
     * @param uri The URI for the request.
     * @param method The HTTP method (e.g., "GET", "POST").
     * @param bodyPublisher The HttpRequest.BodyPublisher for POST/PUT requests, null for GET/DELETE.
     * @return The constructed HttpRequest.Builder.
     */
    private HttpRequest.Builder createAuthorizedRequestBuilder(String uri, String method, HttpRequest.BodyPublisher bodyPublisher) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        if (jwtToken != null && !jwtToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + jwtToken);
        } else {
            System.err.println("WARNING: JWT token is null or empty for a request to: " + uri);
        }

        switch (method.toUpperCase()) {
            case "GET":
                builder.GET();
                break;
            case "POST":
                builder.POST(bodyPublisher);
                break;
            case "PUT":
                builder.PUT(bodyPublisher);
                break;
            case "DELETE":
                builder.DELETE();
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
        return builder;
    }


    /**
     * User Login.
     * @param username The user's username.
     * @param password The user's password.
     * @return A Map containing the JWT token, username, and roles on successful login.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public Map<String, Object> login(String username, String password) throws IOException, InterruptedException { // FIXED: Removed duplicate 'String'
        String json = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", username, password);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // ADDED DEBUGGING: Print raw response body for login
        System.out.println("DEBUG: ApiClient Login Response Status: " + response.statusCode());
        System.out.println("DEBUG: ApiClient Login Raw Response Body: " + response.body());


        if (response.statusCode() == 200) {
            Map<String, Object> responseBody = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>() {}.getType());
            // The 'roles' might come as a List<String> or List<LinkedHashMap> depending on backend JSON structure
            // Ensure proper casting/handling if it's not directly List<String>
            return responseBody;
        } else {
            throw new IOException("Login failed: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    // --- Product Endpoints ---

    /**
     * Retrieves all products or products matching a search query.
     * @param searchQuery Optional search term for product name or description.
     * @return A list of Product objects.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public List<Product> getAllProducts(String searchQuery) throws IOException, InterruptedException {
        String url = BASE_URL + "/products";
        if (searchQuery != null && !searchQuery.isEmpty()) {
            url += "?search=" + URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
        }
        HttpRequest request = createAuthorizedRequestBuilder(url, "GET", null).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            Type productListType = new TypeToken<List<Product>>() {}.getType();
            return gson.fromJson(response.body(), productListType);
        } else {
            throw new IOException("Failed to fetch products: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Creates a new product.
     * @param product The product object to create.
     * @return The created Product object with generated ID.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public Product createProduct(Product product) throws IOException, InterruptedException {
        String json = gson.toJson(product);
        HttpRequest request = createAuthorizedRequestBuilder(BASE_URL + "/products", "POST", HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201) { // 201 Created
            return gson.fromJson(response.body(), Product.class);
        } else {
            throw new IOException("Failed to create product: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Updates an existing product.
     * @param product The product object to update (must have ID).
     * @return The updated Product object.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public Product updateProduct(Product product) throws IOException, InterruptedException {
        String json = gson.toJson(product);
        HttpRequest request = createAuthorizedRequestBuilder(BASE_URL + "/products/" + product.getId(), "PUT", HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), Product.class);
        } else {
            throw new IOException("Failed to update product: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * NEW: Saves a product (either creates or updates based on ID presence).
     * This method simplifies the controller logic by deciding whether to
     * call `createProduct` or `updateProduct`.
     * @param product The product object to save.
     * @return The saved (created or updated) Product object.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public Product saveProduct(Product product) throws IOException, InterruptedException {
        if (product.getId() == null) {
            // If ID is null, it's a new product, so create it
            return createProduct(product);
        } else {
            // If ID exists, it's an existing product, so update it
            return updateProduct(product);
        }
    }

    /**
     * Deletes a product by its ID.
     * @param id The ID of the product to delete.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public void deleteProduct(Long id) throws IOException, InterruptedException {
        HttpRequest request = createAuthorizedRequestBuilder(BASE_URL + "/products/" + id, "DELETE", null).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 204) { // 204 No Content for successful deletion
            throw new IOException("Failed to delete product: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    // --- Receipt/Sales Endpoints ---

    /**
     * DTO for recording a batch sale (receipt) request.
     * Mirrors the backend's RecordReceiptRequest in ReceiptService.
     */
    public static class RecordReceiptRequestDTO {
        private List<SaleItemRequestDTO> saleItems;
        private Receipt.PaymentMethod paymentMethod;
        private double cashAmount;
        private double mpesaAmount;
        private String mpesaTransactionId;

        public RecordReceiptRequestDTO(List<SaleItemRequestDTO> saleItems, Receipt.PaymentMethod paymentMethod, double cashAmount, double mpesaAmount, String mpesaTransactionId) {
            this.saleItems = saleItems;
            this.paymentMethod = paymentMethod;
            this.cashAmount = cashAmount;
            this.mpesaAmount = mpesaAmount;
            this.mpesaTransactionId = mpesaTransactionId;
        }

        // Getters (needed for Gson to serialize)
        public List<SaleItemRequestDTO> getSaleItems() { return saleItems; }
        public Receipt.PaymentMethod getPaymentMethod() { return paymentMethod; }
        public double getCashAmount() { return cashAmount; }
        public double getMpesaAmount() { return mpesaAmount; }
        public String getMpesaTransactionId() { return mpesaTransactionId; }
    }

    /**
     * DTO for an individual sale item within a batch sale request.
     */
    public static class SaleItemRequestDTO {
        private Long productId;
        private int quantity;

        public SaleItemRequestDTO(Long productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        // Getters (needed for Gson to serialize)
        public Long getProductId() { return productId; }
        public int getQuantity() { return quantity; }
    }


    /**
     * Records a new batch sale (receipt) to the backend.
     * @param requestDTO The DTO containing sales items and payment details.
     * @return The created Receipt object.
     * @throws IOException If a network error occurs or backend returns an error.
     * @throws InterruptedException If the operation is interrupted.
     */
    public Receipt recordBatchSale(RecordReceiptRequestDTO requestDTO) throws IOException, InterruptedException {
        String json = gson.toJson(requestDTO);
        // Ensure this URL exactly matches your backend ReceiptController's @RequestMapping and @PostMapping
        HttpRequest request = createAuthorizedRequestBuilder(BASE_URL + "/sales/receipts/batch", "POST", HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201) { // 201 Created
            return gson.fromJson(response.body(), Receipt.class);
        } else {
            throw new IOException("Failed to record batch sale: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Retrieves filtered sales receipts from the backend.
     * @param startDate Optional start date.
     * @param endDate Optional end date.
     * @param cashierId Optional cashier ID.
     * @param paymentMethod Optional payment method.
     * @param productName Optional product name contained in the sale.
     * @return A list of Receipt objects.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public List<Receipt> getFilteredReceipts(LocalDateTime startDate, LocalDateTime endDate, Long cashierId,
                                             Receipt.PaymentMethod paymentMethod, String productName) throws IOException, InterruptedException {
        StringBuilder urlBuilder = new StringBuilder(BASE_URL + "/sales/receipts?"); // THIS IS THE CORRECT BASE URL
        if (startDate != null) {
            urlBuilder.append("startDate=").append(URLEncoder.encode(startDate.format(LocalDateTimeAdapter.FORMATTER), StandardCharsets.UTF_8)).append("&");
        }
        if (endDate != null) {
            urlBuilder.append("endDate=").append(URLEncoder.encode(endDate.format(LocalDateTimeAdapter.FORMATTER), StandardCharsets.UTF_8)).append("&");
        }
        if (cashierId != null) {
            urlBuilder.append("cashierId=").append(cashierId).append("&");
        }
        if (paymentMethod != null) {
            urlBuilder.append("paymentMethod=").append(paymentMethod.name()).append("&");
        }
        if (productName != null && !productName.isEmpty()) {
            urlBuilder.append("productName=").append(URLEncoder.encode(productName, StandardCharsets.UTF_8)).append("&");
        }
        // Remove trailing '&' if any
        String url = urlBuilder.toString();
        if (url.endsWith("&")) {
            url = url.substring(0, url.length() - 1);
        }

        HttpRequest request = createAuthorizedRequestBuilder(url, "GET", null).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            Type receiptListType = new TypeToken<List<Receipt>>() {}.getType();
            return gson.fromJson(response.body(), receiptListType);
        } else {
            throw new IOException("Failed to fetch filtered receipts: HTTP " + response.statusCode() + " - " + response.body());
        }
    }


    /**
     * Deletes a receipt by its ID.
     * @param receiptId The ID of the receipt to delete.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public void deleteReceipt(Long receiptId) throws IOException, InterruptedException {
        HttpRequest request = createAuthorizedRequestBuilder(BASE_URL + "/sales/receipts/" + receiptId, "DELETE", null).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 204) { // 204 No Content for successful deletion
            throw new IOException("Failed to delete receipt: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    // --- User Endpoints ---

    /**
     * Retrieves all users.
     * @return A list of User objects.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public List<User> getAllUsers() throws IOException, InterruptedException {
        String url = BASE_URL + "/users";
        HttpRequest request = createAuthorizedRequestBuilder(url, "GET", null).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            Type userListType = new TypeToken<List<User>>() {}.getType();
            return gson.fromJson(response.body(), userListType);
        } else {
            throw new IOException("Failed to fetch users: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Creates a new user.
     * @param user The user object to create.
     * @return The created User object with generated ID.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public User createUser(User user) throws IOException, InterruptedException {
        String json = gson.toJson(user);
        HttpRequest request = createAuthorizedRequestBuilder(BASE_URL + "/users", "POST", HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201) { // 201 Created
            return gson.fromJson(response.body(), User.class);
        } else {
            throw new IOException("Failed to create user: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Updates an existing user.
     * @param userId The ID of the user to update.
     * @param user The user object with updated details.
     * @return The updated User object.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public User updateUser(Long userId, User user) throws IOException, InterruptedException {
        String json = gson.toJson(user);
        HttpRequest request = createAuthorizedRequestBuilder(BASE_URL + "/users/" + userId, "PUT", HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), User.class);
        } else {
            throw new IOException("Failed to update user: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Deletes a user by their ID.
     * @param id The ID of the user to delete.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public void deleteUser(Long id) throws IOException, InterruptedException {
        HttpRequest request = createAuthorizedRequestBuilder(BASE_URL + "/users/" + id, "DELETE", null).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 204) { // 204 No Content
            throw new IOException("Failed to delete user: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    // --- Stock Adjustment Endpoints ---

    /**
     * DTO for a stock adjustment request.
     */
    public static class StockAdjustmentRequestDTO {
        private Long productId;
        private int quantityChange;
        private String reason;
        private StockAdjustment.AdjustmentType adjustmentType;

        public StockAdjustmentRequestDTO(Long productId, int quantityChange, String reason, StockAdjustment.AdjustmentType adjustmentType) {
            this.productId = productId;
            this.quantityChange = quantityChange;
            this.reason = reason;
            this.adjustmentType = adjustmentType;
        }

        // Getters for Gson serialization
        public Long getProductId() { return productId; }
        public int getQuantityChange() { return quantityChange; }
        public String getReason() { return reason; }
        public StockAdjustment.AdjustmentType getAdjustmentType() { return adjustmentType; }
    }

    /**
     * Adjusts product stock in the backend.
     * @param requestDTO The DTO containing adjustment details.
     * @return The created StockAdjustment record.
     * @throws IOException If a network error occurs or backend returns an error.
     * @throws InterruptedException If the operation is interrupted.
     */
    public StockAdjustment adjustStock(StockAdjustmentRequestDTO requestDTO) throws IOException, InterruptedException {
        String json = gson.toJson(requestDTO);
        HttpRequest request = createAuthorizedRequestBuilder(BASE_URL + "/stock/adjustments", "POST", HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201) { // 201 Created
            return gson.fromJson(response.body(), StockAdjustment.class);
        } else {
            throw new IOException("Failed to adjust stock: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Retrieves all stock adjustment history records.
     * @return A list of StockAdjustment objects.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public List<StockAdjustment> getStockAdjustmentHistory() throws IOException, InterruptedException {
        String url = BASE_URL + "/stock/adjustments/history";
        HttpRequest request = createAuthorizedRequestBuilder(url, "GET", null).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            Type historyListType = new TypeToken<List<StockAdjustment>>() {}.getType();
            return gson.fromJson(response.body(), historyListType);
        } else {
            throw new IOException("Failed to fetch stock adjustment history: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    // --- Category Endpoints ---

    /**
     * Retrieves all categories.
     * @return A list of Category objects.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public List<Category> getAllCategories() throws IOException, InterruptedException {
        String url = BASE_URL + "/categories";
        HttpRequest request = createAuthorizedRequestBuilder(url, "GET", null).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            Type categoryListType = new TypeToken<List<Category>>() {}.getType();
            return gson.fromJson(response.body(), categoryListType);
        } else {
            throw new IOException("Failed to fetch categories: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Creates a new category.
     * @param category The category object to create.
     * @return The created Category object with generated ID.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public Category createCategory(Category category) throws IOException, InterruptedException {
        String json = gson.toJson(category);
        HttpRequest request = createAuthorizedRequestBuilder(BASE_URL + "/categories", "POST", HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201) { // 201 Created
            return gson.fromJson(response.body(), Category.class);
        } else {
            throw new IOException("Failed to create category: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Updates an existing category.
     * @param category The category object to update (must have ID).
     * @return The updated Category object.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public Category updateCategory(Category category) throws IOException, InterruptedException {
        String json = gson.toJson(category);
        HttpRequest request = createAuthorizedRequestBuilder(BASE_URL + "/categories/" + category.getId(), "PUT", HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), Category.class);
        } else {
            throw new IOException("Failed to update category: HTTP " + response.statusCode() + " -  " + response.body());
        }
    }

    /**
     * Deletes a category by its ID.
     * @param id The ID of the category to delete.
     * @throws IOException If a network error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public void deleteCategory(Long id) throws IOException, InterruptedException {
        HttpRequest request = createAuthorizedRequestBuilder(BASE_URL + "/categories/" + id, "DELETE", null).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 204) { // 204 No Content
            throw new IOException("Failed to delete category: HTTP " + response.statusCode() + " - " + response.body());
        }
    }
}
