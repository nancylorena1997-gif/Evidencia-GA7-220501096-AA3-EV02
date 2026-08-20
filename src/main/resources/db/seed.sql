INSERT OR IGNORE INTO cuenta (idcuenta, contrasena, email) VALUES
  (1, 'admin123', 'admin@newphone.com'),
  (2, 'cliente123', 'cliente@newphone.com'),
  (3, 'laura123', 'laura@newphone.com');

INSERT OR IGNORE INTO administrador (cedula, nombre, permisos, telefono, cuenta_idcuenta) VALUES
  (1001, 'Ana Administradora', 'TOTAL', '3001001001', 1);

INSERT OR IGNORE INTO cliente (cedula, nombre, telefono, registro, cuenta_idcuenta) VALUES
  (2001, 'Carlos Ramírez', '3002002001', '2026-01-15', 2),
  (2002, 'Laura Gómez', '3002002002', '2026-03-04', 3);

INSERT OR IGNORE INTO categoria (id_categoria, nombre) VALUES
  (1, 'Smartphones'),
  (2, 'Accesorios'),
  (3, 'Audio');

INSERT OR IGNORE INTO producto
  (idproducto, nombre, precio, stock, descripcion, administrador_cedula, categoria_id_categoria)
VALUES
  (1, 'NewPhone X Pro', 5299900, 25, 'Pantalla AMOLED, cámara profesional y rendimiento de nueva generación.', 1001, 1),
  (2, 'NewPhone Air', 2899900, 42, 'Ligero, potente y diseñado para acompañarte todo el día.', 1001, 1),
  (3, 'Funda MagSafe', 89900, 100, 'Protección premium con acople magnético.', 1001, 2),
  (4, 'NewBuds Pro', 649900, 36, 'Audio inmersivo con cancelación activa de ruido.', 1001, 3),
  (5, 'Cargador 45W', 159900, 8, 'Carga rápida USB-C con protección inteligente.', 1001, 2);

INSERT OR IGNORE INTO pedido (idpedido, fecha, estado, total, cliente_cedula) VALUES
  (1, '2026-07-18', 'Entregado', 5389800, 2001),
  (2, '2026-07-24', 'Preparado', 3549800, 2002),
  (3, '2026-07-26', 'Enviado', 809800, 2001);

INSERT OR IGNORE INTO detalle_pedido
  (iddetalle_pedido, cantidad, subtotal, pedido_idpedido, producto_idproducto)
VALUES
  (1, 1, 5299900, 1, 1),
  (2, 1, 89900, 1, 3),
  (3, 1, 2899900, 2, 2),
  (4, 1, 649900, 2, 4),
  (5, 1, 649900, 3, 4),
  (6, 1, 159900, 3, 5);

INSERT OR IGNORE INTO carrito (idcarrito, fecha, cliente_cedula) VALUES
  (1, '2026-07-27', 2001);

INSERT OR IGNORE INTO detalle_carrito
  (iddetalle_carrito, subtotal, cantidad, precio_unitario, producto_idproducto, carrito_idcarrito)
VALUES
  (1, 179800, 2, 89900, 3, 1);

INSERT OR IGNORE INTO envio
  (id_envio, direccion, fecha_envio, estado_envio, pedido_idpedido)
VALUES
  (1, 'Calle 24 # 18-30, Bogotá', '2026-07-19', 'Entregado', 1),
  (2, 'Carrera 7 # 61-15, Medellín', '2026-07-27', 'Enviado', 3),
  (3, 'Calle 10 # 4-21, Cali', '2026-07-24', 'Preparado', 2);

UPDATE envio SET
  guia = 'NP-20260719-000001',
  telefono = '3002002001',
  destinatario = 'Carlos Ramírez'
WHERE id_envio = 1 AND (guia IS NULL OR guia = '');

UPDATE envio SET
  guia = 'NP-20260727-000003',
  telefono = '3002002001',
  destinatario = 'Carlos Ramírez'
WHERE id_envio = 2 AND (guia IS NULL OR guia = '');

UPDATE envio SET
  guia = 'NP-20260724-000002',
  telefono = '3002002002',
  destinatario = 'Laura Gómez'
WHERE id_envio = 3 AND (guia IS NULL OR guia = '');

INSERT OR IGNORE INTO factura
  (idfactura, fecha, iva, descuento, total, pedido_idpedido)
VALUES
  (1, '2026-07-18', 860731.09, 0, 5389800, 1),
  (2, '2026-07-24', 566691.60, 0, 3549800, 2);

INSERT OR IGNORE INTO pagos (idpagos, metodo, estado_pago, pedido_idpedido) VALUES
  (1, 'Tarjeta de crédito', 'Aprobado', 1),
  (2, 'PSE', 'Aprobado', 2),
  (3, 'Nequi', 'Aprobado', 3);

INSERT OR IGNORE INTO resena
  (id_resena, comentario, calificacion, cliente_cedula, producto_idproducto)
VALUES
  (1, 'La pantalla y la cámara son espectaculares.', 5, 2001, 1),
  (2, 'Muy buen sonido y la batería dura bastante.', 4, 2002, 4),
  (3, 'La funda se siente premium y el MagSafe funciona bien.', 5, 2001, 3);

INSERT OR IGNORE INTO atencion_cliente
  (id_ticket, mensaje, fecha, respuesta, cliente_cedula)
VALUES
  (1, '¿Cómo consulto el estado de mi envío?', '2026-07-25', 'Puedes revisarlo desde el módulo de envíos.', 2001),
  (2, 'Necesito cambiar la dirección de entrega.', '2026-07-27', NULL, 2002);

INSERT OR IGNORE INTO producto_favorito
  (idproducto_favorito, fecha_agregado, producto_idproducto, cliente_cedula)
VALUES
  (1, '2026-07-20', 1, 2001),
  (2, '2026-07-22', 4, 2002);

INSERT OR IGNORE INTO reportes_venta
  (idreportes_venta, fecha_inicial, fecha_final, tipo_reporte, total_ventas,
   cantidad_pedidos, archivo_descargable, analisis_venta, administrador_cedula)
VALUES
  (1, '2026-07-01', '2026-07-31', 'Mensual', 9749400, 3, 'ventas-julio.csv',
   'Smartphones representa la categoría con mayor facturación.', 1001),
  (2, '2026-01-01', '2026-01-31', 'Mensual', 4125800, 2, 'ventas-enero.csv',
   'El arranque del año se concentró en smartphones de gama media.', 1001),
  (3, '2026-02-01', '2026-02-28', 'Mensual', 3562100, 2, 'ventas-febrero.csv',
   'Los accesorios MagSafe acompañaron la mayoría de las compras.', 1001),
  (4, '2026-03-01', '2026-03-31', 'Mensual', 5218700, 4, 'ventas-marzo.csv',
   'Audio y audífonos crecieron por campañas de primavera.', 1001),
  (5, '2026-04-01', '2026-04-30', 'Mensual', 2896400, 2, 'ventas-abril.csv',
   'La demanda se estabilizó con recargas de accesorios.', 1001),
  (6, '2026-05-01', '2026-05-31', 'Mensual', 6341200, 5, 'ventas-mayo.csv',
   'El NewPhone Air impulsó el ticket promedio del mes.', 1001),
  (7, '2026-06-01', '2026-06-30', 'Mensual', 4789300, 3, 'ventas-junio.csv',
   'Cargadores y fundas cerraron el primer semestre con buen ritmo.', 1001),
  (8, '2026-01-01', '2026-03-31', 'Trimestral', 12906600, 8, 'ventas-q1.csv',
   'El primer trimestre consolidó a Smartphones como categoría líder.', 1001),
  (9, '2026-04-01', '2026-06-30', 'Trimestral', 14026900, 10, 'ventas-q2.csv',
   'El segundo trimestre superó a Q1 por el despegue de audio.', 1001),
  (10, '2026-06-29', '2026-07-05', 'Semanal', 2184500, 2, 'ventas-semana-27.csv',
   'La última semana de junio cerró con dos pedidos de alto valor.', 1001),
  (11, '2026-07-20', '2026-07-26', 'Semanal', 4359600, 2, 'ventas-semana-30.csv',
   'La semana 30 concentró pedidos en preparación y envío.', 1001),
  (12, '2026-07-18', '2026-07-18', 'Diario', 5389800, 1, 'ventas-2026-07-18.csv',
   'Un único pedido entregado de NewPhone X Pro y funda MagSafe.', 1001);
