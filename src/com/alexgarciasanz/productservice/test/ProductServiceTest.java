package com.alexgarciasanz.productservice.test;

import com.alexgarciasanz.productservice.dao.ProductDAO;
import com.alexgarciasanz.productservice.dao.ProductDAOImpl;
import com.alexgarciasanz.productservice.model.Product;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductServiceTest {

    // --- Parámetros de la Simulación ---
    private static final int STOCK_INICIAL = 1000;
    private static final int PRODUCT_ID = 1;
    private static final int HILOS = 10; // 10 hilos concurrentes
    private static final int DEDUCCIONES_POR_HILO = 20; // Cada hilo descuenta 20 veces
    private static final int CANTIDAD_POR_DEDUCCION = 5; // Cada vez descuenta 5 unidades

    // Cálculo esperado
    // 10 hilos * 20 deducciones * 5 unidades = 1000 unidades
    // Stock Final Esperado: 1000 (inicial) - 1000 (deducido) = 0
    private static final int STOCK_FINAL_ESPERADO = STOCK_INICIAL - (HILOS * DEDUCCIONES_POR_HILO * CANTIDAD_POR_DEDUCCION);

    public static void main(String[] args) throws InterruptedException {
        ProductDAO dao = new ProductDAOImpl();

        // Ejecutar la simulación insegura
        runSimulation(dao, false);

        System.out.println("\n" + "=".repeat(50) + "\n");

        // Ejecutar la simulación segura
        runSimulation(dao, true);
    }

    private static void runSimulation(ProductDAO dao, boolean useSafeMethod) throws InterruptedException {
        
        //  Inicializar la Base de Datos y el Producto
        dao.setupDatabase();
        dao.addProduct(new Product(PRODUCT_ID, "P100", STOCK_INICIAL, "WH1"));

        if (useSafeMethod) {
            System.out.println("INICIANDO SIMULACIÓN SEGURA (Con Transacciones y Bloqueo)");
        } else {
            System.out.println("INICIANDO SIMULACIÓN INSEGURA (Sin Transacciones)");
        }

        // Usamos un ExecutorService para manejar el pool de hilos
        ExecutorService executor = Executors.newFixedThreadPool(HILOS);
        
        // Usamos un CountDownLatch para saber cuándo han terminado todos los hilos
        CountDownLatch latch = new CountDownLatch(HILOS * DEDUCCIONES_POR_HILO);

        long startTime = System.currentTimeMillis();

        //  Lanzar los hilos
        for (int i = 0; i < HILOS; i++) {
            executor.submit(() -> {
                for (int j = 0; j < DEDUCCIONES_POR_HILO; j++) {
                    try {
                        if (useSafeMethod) {
                            dao.deductStockSafe(PRODUCT_ID, CANTIDAD_POR_DEDUCCION);
                        } else {
                            dao.deductStockUnsafe(PRODUCT_ID, CANTIDAD_POR_DEDUCCION);
                        }
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        // Esperar a que todos los hilos terminen
        latch.await();
        executor.shutdown();
        long endTime = System.currentTimeMillis();

        //  Mostrar Resultados
        Product finalProduct = dao.getProductById(PRODUCT_ID);
        
        System.out.println("--- RESULTADOS ---");
        System.out.println("Tiempo total: " + (endTime - startTime) + " ms");
        System.out.println("Stock Inicial: " + STOCK_INICIAL);
        System.out.println("Stock Final Esperado: " + STOCK_FINAL_ESPERADO);
        System.out.println("Stock Final REAL en BD: " + finalProduct.getStock());

        if (finalProduct.getStock() == STOCK_FINAL_ESPERADO) {
            System.out.println("✅ RESULTADO: CORRECTO (Consistente)");
        } else {
            System.out.println("❌ RESULTADO: INCORRECTO (Inconsistente)");
        }
    }
}