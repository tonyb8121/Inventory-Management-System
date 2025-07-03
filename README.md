# Inventory Management System (JavaFX & Spring Boot)

This is a **comprehensive inventory management system** designed to track products, manage stock levels, record sales, and provide dashboard insights. The application is split into two main components: a **Spring Boot backend (REST API)** and a **JavaFX desktop frontend**.

## Features

### Product Management
- **Add New Products**: Easily add new items to your inventory with details like name, description, price, quantity, and minimum stock level.
- **Edit Existing Products**: Update product details as needed.
- **Delete Products**: Remove products from the inventory. The system **prevents deletion of products that have associated sales records** to maintain data integrity.
- **Product Search**: Quickly find products by name in the main product catalog.
- **Low Stock Alerts**: Products falling below their minimum stock level are **visually highlighted**.

### Sales Management
- **Record Sales**: Process new sales by selecting a product, entering the quantity, and automatically calculating the total amount. **Stock levels are updated automatically**. The product selection dropdown now displays both name and description for better clarity (e.g., "Cooking Oil (1L)" vs "Cooking Oil (2L)").
- **Sales History**: View a chronological list of all sales records. The sales history table is **optimized to display more records** without excessive scrolling.
- **Search Sales History**: Filter sales records by product name.
- **Delete Sales Records**: Remove individual sale transactions. This action **returns the sold quantity back to the product's stock**.
- **PIN Authorization for Deletion**: A security measure requiring a **specific PIN to authorize** the deletion of sales records.

### Dashboard
- **Key Metrics Overview**: Displays summary information such as total number of products and total sales amount.
- **Low Stock Products List**: Provides a quick view of all products requiring attention due to low stock.

## Technologies Used

### Backend (Spring Boot)
- **Java 17+**: Programming language
- **Spring Boot 3.3.1**: Framework for building robust RESTful APIs
- **Spring Data JPA**: For simplified database access and ORM
- **H2 Database**: An in-memory database used for development and easy setup (**Configured for file-based persistence** by default for data retention)
- **RESTful API**: Provides endpoints for all CRUD operations and data retrieval

### Frontend (JavaFX)
- **Java 17+**: Programming language
- **JavaFX 22**: UI toolkit for building rich desktop applications
- **FXML**: XML-based language for defining the user interface
- **HTTPClient**: For making asynchronous HTTP requests to the backend API
- **Gson**: For JSON serialization and deserialization between frontend and backend

## Architecture

The application follows a **client-server architecture**:

- **Backend**: A Spring Boot application that exposes a RESTful API. It handles business logic, data persistence (using H2 database), and serves data to the frontend.
- **Frontend**: A JavaFX desktop application that consumes the backend API. It provides the graphical user interface for users to interact with the inventory system.

## Setup and Running the Project

### Prerequisites
To run this project, you will need:
- **Java Development Kit (JDK) 17 or higher**
- **Maven**: A build automation tool. Most modern Java IDEs come with Maven integrated
- **Git**: For cloning the repository
- **An IDE**: IntelliJ IDEA (recommended), Eclipse, or VS Code with Java extensions

⚠️ **If using IntelliJ IDEA** to run JavaFX directly (without Maven terminal commands), you'll also need to **download the JavaFX SDK separately** (matching your JDK version).

### 1. Clone the Repository
First, download the project by cloning the Git repository:
```bash
git clone https://github.com/tonyb8121/Inventory-Management-System.git
cd Inventory-Management-System
```

### 2. Run the Backend (Spring Boot Application)
⚠️ **The backend must be running before you start the frontend.**

#### Via IntelliJ IDEA (Recommended for non-terminal users):
1. **Open Project**: Open IntelliJ IDEA. Go to `File -> Open...` and select the cloned `Inventory-Management-System` folder. IntelliJ will usually detect it as a Maven project.
2. **Navigate to Main Class**: In the project explorer, navigate to `inventory-backend/src/main/java/com/inventory/InventoryApplication.java`.
3. **Run**: Right-click on the `InventoryApplication.java` file and select `Run 'InventoryApplication.main()'`.

This will start the Spring Boot backend server.

#### Via Terminal (for any IDE user, or if running without an IDE):
1. **Open Terminal**: Open your terminal or command prompt.
2. **Navigate to Backend Directory**:
   ```bash
   cd /path/to/your/cloned/repo/Inventory-Management-System/inventory-backend
   ```
3. **Run with Maven**:
   ```bash
   mvn spring-boot:run
   ```

The backend will start on **http://localhost:8080**.

💡 You can optionally access the H2 database console in your web browser at **http://localhost:8080/h2-console** (use JDBC URL: `jdbc:h2:file:./data/inventorydb`, Username: `sa`, Password: leave blank).

### 3. Run the Frontend (JavaFX Application)
⚠️ **The frontend connects to the backend, so ensure the backend is running first.**

#### Via IntelliJ IDEA (Recommended for non-terminal users):
This method requires a bit more setup in IntelliJ for JavaFX projects, but avoids using the terminal.

1. **Open Project**: Ensure the `Inventory-Management-System` project is open in IntelliJ IDEA.

2. **Configure JavaFX SDK** (One-time setup per project):
   - Go to `File -> Project Structure...` (Ctrl+Alt+Shift+S on Windows/Linux or Cmd+; on macOS).
   - In the "Project Structure" window:
     - Under `Platform Settings -> SDKs`, ensure your JDK 17+ is selected.
     - Under `Project Settings -> Libraries`, click the `+` button, then select `Java`.
     - Navigate to your downloaded JavaFX SDK folder and select the `lib` directory (e.g., `javafx-sdk-22/lib`). Click OK.
     - Confirm the library is added. In the modules section on the right, make sure this JavaFX library is attached to the `inventory-javafx-app` module.

3. **Configure Run Configuration**:
   - Go to `Run -> Edit Configurations...`.
   - In the left panel, find `Application -> InventoryApp`. If it doesn't exist, click `+` -> `Application` and set Main class to `com.inventory.ui.InventoryApp` and Use classpath of module to `inventory-javafx-app`.
   - In the VM options field, add the following (replace `/path/to/javafx-sdk/lib` with the actual path to your downloaded JavaFX SDK lib directory):
     ```
     --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
     ```
   - Click Apply and OK.

4. **Run**: Navigate to `inventory-javafx-app/src/main/java/com/inventory/ui/InventoryApp.java`.
5. **Run**: Right-click on the `InventoryApp.java` file and select `Run 'InventoryApp.main()'`.

The JavaFX application window should now appear.

Login Credentials:

**owner**- Password is: **password**

**cashier** -Password is: **password**

#### Via Terminal (Recommended for quick runs and other IDE users):
This method uses the **JavaFX Maven Plugin**, which simplifies dependencies and module path handling.

1. **Open Terminal**: Open a new terminal or command prompt.
2. **Navigate to Frontend Directory**:
   ```bash
   cd /path/to/your/cloned/repo/Inventory-Management-System/inventory-javafx-app
   ```
3. **Run with JavaFX Maven Plugin**:
   ```bash
   mvn javafx:run
   ```

The JavaFX application window should appear.

## Important Notes for Users:
- **Java Version**: Ensure your JDK version is **17 or higher**.
- **Maven**: Make sure Maven is correctly set up in your environment and recognized by your IDE.
- **Network**: The frontend needs to connect to the backend, so **both applications must be running simultaneously** and accessible on localhost:8080.
- **Sales Deletion PIN**: The default PIN to delete sales records is **1234**.
- **Data Persistence**: The H2 database saves data to a file (`./data/inventorydb.mv.db`) in the backend's working directory (`inventory-backend/data/`), so **data will persist across restarts**.
