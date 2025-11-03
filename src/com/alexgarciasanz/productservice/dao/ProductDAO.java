package com.alexgarciasanz.productservice.dao;

import com.alexgarciasanz.productservice.model.Product;

public interface ProductDAO {

    /**
     * Crea la tabla de productos .
     */
    void setupDatabase();

    /**
     * Añade un nuevo producto a la Base de Datos.
     */
    void addProduct(Product product);

    /**
     * Obtiene un producto por su ID.
     */
    Product getProductById(int id);

    /**
     * Deduce el stock SIN gestión de transacciones ni bloqueos.
     * Este método es propenso a 'Race Conditions'.
     */
    void deductStockUnsafe(int id, int quantity);

    /**
     * Deduce el stock de forma segura usando transacciones
     * y bloqueo pesimista (SELECT ... FOR UPDATE).
     */
    void deductStockSafe(int id, int quantity);
}