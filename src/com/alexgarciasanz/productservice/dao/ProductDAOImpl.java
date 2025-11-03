package com.alexgarciasanz.productservice.dao;

import com.alexgarciasanz.productservice.model.Product;
import com.alexgarciasanz.productservice.util.DBConnection;

import java.sql.*;

public class ProductDAOImpl implements ProductDAO {

    @Override
    public void setupDatabase() {
        String dropTable = "DROP TABLE IF EXISTS products;";
        String createTable = "CREATE TABLE products (" +
                "id INT PRIMARY KEY, " +
                "productCode VARCHAR(255), " +
                "stock INT, " +
                "warehouse VARCHAR(255));";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(dropTable);
            stmt.execute(createTable);
            System.out.println("Tabla 'products' creada con éxito.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addProduct(Product product) {
        String sql = "INSERT INTO products (id, productCode, stock, warehouse) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, product.getId());
            pstmt.setString(2, product.getProductCode());
            pstmt.setInt(3, product.getStock());
            pstmt.setString(4, product.getWarehouse());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Product getProductById(int id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        Product product = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    product = new Product();
                    product.setId(rs.getInt("id"));
                    product.setProductCode(rs.getString("productCode"));
                    product.setStock(rs.getInt("stock"));
                    product.setWarehouse(rs.getString("warehouse"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return product;
    }

    @Override
    public void deductStockUnsafe(int id, int quantity) {
        try {
            // 1. LEER el stock actual
            Product product = getProductById(id);
            if (product == null || product.getStock() < quantity) {
                // No hay suficiente stock o el producto no existe
                return;
            }
            int currentStock = product.getStock();

            // Añadimos un pequeño retardo para forzar que otros hilos
            // lean el valor 'currentStock' (100) antes de que este hilo escriba.
            Thread.sleep(10); // <-- ESTO PROVOCA LA RACE CONDITION

            // CALCULAR y ESCRIBIR el nuevo stock
            int newStock = currentStock - quantity;
            String sql = "UPDATE products SET stock = ? WHERE id = ?";

            // Cada conexión aquí tiene auto-commit=true por defecto.
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, newStock);
                pstmt.setInt(2, id);
                pstmt.executeUpdate();
            }

        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void deductStockSafe(int id, int quantity) {
        String selectForUpdate = "SELECT stock FROM products WHERE id = ? FOR UPDATE";
        String updateStock = "UPDATE products SET stock = ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            // Desactivar el Auto-commit para iniciar una transacción
            conn.setAutoCommit(false);
            
            // Opcional: Asegurar un nivel de aislamiento adecuado
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            int currentStock = -1;

            // 1. LEER y BLOQUEAR la fila
            try (PreparedStatement psSelect = conn.prepareStatement(selectForUpdate)) {
                psSelect.setInt(1, id);
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        currentStock = rs.getInt("stock");
                    }
                }
            }

            if (currentStock < quantity) {
                System.out.println("Stock insuficiente, revirtiendo.");
                conn.rollback(); // Revierte la transacción
                return;
            }

            // *** Simulación de retardo ***
            // Aunque pongamos un sleep, el Hilo B no puede leer
            // porque la fila está bloqueada por el Hilo A.
            Thread.sleep(10);

            // 2. CALCULAR y ESCRIBIR el nuevo stock
            int newStock = currentStock - quantity;
            try (PreparedStatement psUpdate = conn.prepareStatement(updateStock)) {
                psUpdate.setInt(1, newStock);
                psUpdate.setInt(2, id);
                psUpdate.executeUpdate();
            }

            // 3. Confirmar la transacción
            conn.commit();

        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
            // Si algo falla, revertir la transacción
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            // Siempre cerrar la conexión y restaurar auto-commit
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}