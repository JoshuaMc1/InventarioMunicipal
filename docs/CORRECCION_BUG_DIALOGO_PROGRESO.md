# Corrección: Bug del Diálogo de Progreso que no se Cierra

## 📅 Fecha: 27 de Noviembre de 2025

## 🐛 Problema Reportado

Al generar un PDF de recibo, el diálogo "Generando PDF..." no se cerraba automáticamente y permanecía bloqueado hasta que se terminaba el proceso de la aplicación manualmente.

### Síntomas:
- Diálogo "Generando PDF..." quedaba visible indefinidamente
- El PDF se generaba correctamente (visible en el sistema de archivos)
- El diálogo no respondía a clics
- Solo se podía cerrar terminando el proceso de la aplicación

## 🔍 Causa del Bug

El problema estaba en el uso de `showAndWait()` en lugar de `show()` para mostrar el diálogo de progreso.

### Código Problemático:

```java
private static void showProgressAndGeneratePdf(Transaction transaction, String pdfPath) {
    Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
    // ... configuración del diálogo ...
    
    Thread pdfThread = new Thread(() -> {
        try {
            LoanReceiptService.generatePdfReceiptFromDTO(receiptDTO, pdfPath);
            
            Platform.runLater(() -> {
                progressAlert.close();  // ❌ Intenta cerrar desde otro hilo
                // ... mostrar mensaje de éxito ...
            });
        }
    });
    pdfThread.start();
    
    progressAlert.showAndWait();  // ❌ PROBLEMA: Bloquea hasta que se cierre
}
```

### ¿Por qué no funcionaba?

1. **`showAndWait()` bloquea el hilo principal:**
   - Este método bloquea la ejecución hasta que el diálogo se cierre
   - El hilo principal queda esperando

2. **El cierre se intenta desde un hilo secundario:**
   - `Platform.runLater(() -> progressAlert.close())` se ejecuta en el hilo de JavaFX
   - Pero el hilo principal está bloqueado esperando en `showAndWait()`

3. **Resultado: Deadlock suave:**
   - El diálogo espera ser cerrado
   - El código que lo cierra nunca puede ejecutarse correctamente
   - El diálogo queda "congelado"

### Diagrama del Problema:

```
┌─────────────────────────────────────┐
│      Hilo Principal (JavaFX)        │
│                                     │
│  progressAlert.showAndWait()       │
│          ⬇                          │
│    [BLOQUEADO ESPERANDO]           │
│          ⬇                          │
│    ❌ No puede continuar            │
│                                     │
└─────────────────────────────────────┘
                │
                │ Intenta cerrar
                ▼
┌─────────────────────────────────────┐
│      Hilo Secundario (Thread)       │
│                                     │
│  Platform.runLater(() -> {         │
│      progressAlert.close()         │
│  })                                 │
│          ⬇                          │
│    ❌ No puede cerrar porque        │
│       showAndWait() está bloqueado │
│                                     │
└─────────────────────────────────────┘
```

## ✅ Solución Implementada

Cambiar `showAndWait()` por `show()` para permitir que el diálogo se muestre sin bloquear el hilo principal.

### Código Corregido:

```java
private static void showProgressAndGeneratePdf(Transaction transaction, String pdfPath) {
    Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
    // ... configuración del diálogo ...
    
    Thread pdfThread = new Thread(() -> {
        try {
            LoanReceiptService.generatePdfReceiptFromDTO(receiptDTO, pdfPath);
            
            Platform.runLater(() -> {
                progressAlert.close();  // ✅ Ahora puede cerrar correctamente
                // ... mostrar mensaje de éxito ...
            });
        }
    });
    pdfThread.start();
    
    progressAlert.show();  // ✅ SOLUCIÓN: No bloquea el hilo
}
```

### Diagrama de la Solución:

```
┌─────────────────────────────────────┐
│      Hilo Principal (JavaFX)        │
│                                     │
│  progressAlert.show()              │
│          ⬇                          │
│    ✅ Continúa ejecutando           │
│          ⬇                          │
│    Hilo disponible para eventos    │
│                                     │
└─────────────────────────────────────┘
                │
                │ Puede cerrar
                ▼
┌─────────────────────────────────────┐
│      Hilo Secundario (Thread)       │
│                                     │
│  Platform.runLater(() -> {         │
│      progressAlert.close()         │
│  })                                 │
│          ⬇                          │
│    ✅ Cierra correctamente          │
│                                     │
└─────────────────────────────────────┘
```

## 🔧 Archivos Modificados

### PrintReceiptDialog.java

Se modificaron dos métodos:

#### 1. `showProgressAndGeneratePdf()`
**Línea modificada:**
```java
// Antes:
progressAlert.showAndWait();

// Después:
progressAlert.show();
```

#### 2. `showProgressAndPrint()`
**Línea modificada:**
```java
// Antes:
progressAlert.showAndWait();

// Después:
progressAlert.show();
```

## 📊 Diferencia: show() vs showAndWait()

| Característica | `show()` | `showAndWait()` |
|----------------|----------|-----------------|
| **Bloqueo** | No bloquea | Bloquea el hilo |
| **Retorno** | Retorna inmediatamente | Retorna cuando se cierra |
| **Uso típico** | Diálogos de progreso | Diálogos de confirmación |
| **Cierre desde otro hilo** | ✅ Funciona | ❌ Problemas |
| **Modeless** | Sí | No |

## 🎯 Comportamiento Correcto Ahora

### Flujo de Generación de PDF:

1. Usuario hace clic en "Emitir Recibo" → "Guardar como PDF"
2. Se muestra diálogo "Generando PDF..." (no bloqueante)
3. Hilo secundario genera el PDF
4. Al terminar, el hilo secundario cierra el diálogo de progreso
5. Se muestra diálogo de éxito con opciones
6. Usuario puede abrir la ubicación o cerrar

### Ventajas de la Corrección:

✅ **Diálogo se cierra automáticamente** cuando termina la generación
✅ **No requiere intervención manual** del usuario
✅ **Experiencia de usuario fluida** sin bloqueos
✅ **Consistente** con el comportamiento esperado de diálogos de progreso
✅ **No afecta** la generación del PDF (sigue funcionando igual)

## 🧪 Pruebas Realizadas

### Escenario 1: Generación Exitosa de PDF
1. Crear préstamo
2. Click en "Emitir Recibo"
3. Seleccionar "Guardar como PDF"
4. Elegir ubicación
5. Click en "Emitir"

**Resultado esperado:**
- ✅ Aparece "Generando PDF..."
- ✅ Se genera el PDF
- ✅ Diálogo se cierra automáticamente
- ✅ Aparece mensaje de éxito

### Escenario 2: Error en Generación
1. Seleccionar ubicación inválida (sin permisos)
2. Click en "Emitir"

**Resultado esperado:**
- ✅ Aparece "Generando PDF..."
- ✅ Ocurre error
- ✅ Diálogo se cierra automáticamente
- ✅ Aparece mensaje de error

### Escenario 3: Impresión en Impresora Térmica
1. Seleccionar impresora
2. Click en "Emitir"

**Resultado esperado:**
- ✅ Aparece "Imprimiendo..."
- ✅ Se imprime el recibo
- ✅ Diálogo se cierra automáticamente
- ✅ Aparece mensaje de éxito

## 📝 Notas Técnicas

### Cuándo usar `show()` vs `showAndWait()`

**Usar `show()`:**
- Diálogos de progreso
- Notificaciones que se cierran solas
- Cuando el resultado se procesa en otro hilo
- Cuando no necesitas bloquear la ejecución

**Usar `showAndWait()`:**
- Diálogos de confirmación (Sí/No)
- Diálogos de entrada de datos
- Cuando necesitas el resultado antes de continuar
- Cuando el usuario debe tomar una decisión

### Pattern Utilizado

Este es un patrón común en JavaFX para operaciones asíncronas:

```java
// 1. Mostrar diálogo de progreso (no bloqueante)
Alert progress = new Alert(...);
progress.show();

// 2. Ejecutar operación en hilo secundario
new Thread(() -> {
    // Hacer trabajo pesado
    
    // 3. Actualizar UI desde el hilo de JavaFX
    Platform.runLater(() -> {
        progress.close();
        // Mostrar resultado
    });
}).start();
```

## ✅ Estado Actual

- ✅ Bug corregido
- ✅ Compilación exitosa
- ✅ Ambos métodos actualizados (PDF e impresión)
- ✅ Comportamiento consistente
- ✅ Experiencia de usuario mejorada

## 🎉 Conclusión

El bug del diálogo que no se cerraba ha sido resuelto completamente. El cambio fue simple pero crucial: usar `show()` en lugar de `showAndWait()` para diálogos de progreso que se cierran desde hilos secundarios.

Esta es una lección importante sobre:
- La diferencia entre operaciones bloqueantes y no bloqueantes
- El manejo correcto de hilos en JavaFX
- La importancia de elegir el método correcto para cada situación

