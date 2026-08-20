PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS cuenta (
  idcuenta INTEGER PRIMARY KEY AUTOINCREMENT,
  contrasena TEXT NOT NULL,
  email TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS cliente (
  cedula INTEGER PRIMARY KEY,
  nombre TEXT NOT NULL,
  telefono TEXT NOT NULL UNIQUE,
  registro TEXT NOT NULL,
  cuenta_idcuenta INTEGER,
  FOREIGN KEY (cuenta_idcuenta) REFERENCES cuenta(idcuenta)
);

CREATE TABLE IF NOT EXISTS administrador (
  cedula INTEGER PRIMARY KEY,
  nombre TEXT NOT NULL,
  permisos TEXT NOT NULL,
  telefono TEXT NOT NULL UNIQUE,
  cuenta_idcuenta INTEGER,
  FOREIGN KEY (cuenta_idcuenta) REFERENCES cuenta(idcuenta)
);

CREATE TABLE IF NOT EXISTS categoria (
  id_categoria INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS producto (
  idproducto INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre TEXT NOT NULL,
  precio REAL NOT NULL CHECK (precio >= 0),
  stock INTEGER NOT NULL CHECK (stock >= 0),
  descripcion TEXT NOT NULL,
  imagen BLOB,
  administrador_cedula INTEGER,
  categoria_id_categoria INTEGER,
  FOREIGN KEY (administrador_cedula) REFERENCES administrador(cedula),
  FOREIGN KEY (categoria_id_categoria) REFERENCES categoria(id_categoria)
);

CREATE TABLE IF NOT EXISTS pedido (
  idpedido INTEGER PRIMARY KEY AUTOINCREMENT,
  fecha TEXT NOT NULL,
  estado TEXT NOT NULL,
  total REAL NOT NULL CHECK (total >= 0),
  cliente_cedula INTEGER,
  FOREIGN KEY (cliente_cedula) REFERENCES cliente(cedula)
);

CREATE TABLE IF NOT EXISTS detalle_pedido (
  iddetalle_pedido INTEGER PRIMARY KEY AUTOINCREMENT,
  cantidad INTEGER NOT NULL CHECK (cantidad > 0),
  subtotal REAL NOT NULL CHECK (subtotal >= 0),
  pedido_idpedido INTEGER,
  producto_idproducto INTEGER,
  FOREIGN KEY (pedido_idpedido) REFERENCES pedido(idpedido),
  FOREIGN KEY (producto_idproducto) REFERENCES producto(idproducto)
);

CREATE TABLE IF NOT EXISTS carrito (
  idcarrito INTEGER PRIMARY KEY AUTOINCREMENT,
  fecha TEXT NOT NULL,
  cliente_cedula INTEGER,
  FOREIGN KEY (cliente_cedula) REFERENCES cliente(cedula)
);

CREATE TABLE IF NOT EXISTS detalle_carrito (
  iddetalle_carrito INTEGER PRIMARY KEY AUTOINCREMENT,
  subtotal REAL NOT NULL CHECK (subtotal >= 0),
  cantidad INTEGER NOT NULL CHECK (cantidad > 0),
  precio_unitario REAL NOT NULL CHECK (precio_unitario >= 0),
  producto_idproducto INTEGER,
  carrito_idcarrito INTEGER,
  FOREIGN KEY (producto_idproducto) REFERENCES producto(idproducto),
  FOREIGN KEY (carrito_idcarrito) REFERENCES carrito(idcarrito)
);

CREATE TABLE IF NOT EXISTS atencion_cliente (
  id_ticket INTEGER PRIMARY KEY AUTOINCREMENT,
  mensaje TEXT NOT NULL,
  fecha TEXT NOT NULL,
  respuesta TEXT,
  cliente_cedula INTEGER,
  FOREIGN KEY (cliente_cedula) REFERENCES cliente(cedula)
);

CREATE TABLE IF NOT EXISTS resena (
  id_resena INTEGER PRIMARY KEY AUTOINCREMENT,
  comentario TEXT NOT NULL,
  calificacion INTEGER NOT NULL CHECK (calificacion BETWEEN 1 AND 5),
  imagen BLOB,
  cliente_cedula INTEGER,
  producto_idproducto INTEGER,
  FOREIGN KEY (cliente_cedula) REFERENCES cliente(cedula),
  FOREIGN KEY (producto_idproducto) REFERENCES producto(idproducto),
  UNIQUE (cliente_cedula, producto_idproducto)
);

CREATE TABLE IF NOT EXISTS envio (
  id_envio INTEGER PRIMARY KEY AUTOINCREMENT,
  direccion TEXT NOT NULL,
  fecha_envio TEXT NOT NULL,
  estado_envio TEXT NOT NULL,
  pedido_idpedido INTEGER,
  guia TEXT UNIQUE,
  telefono TEXT,
  destinatario TEXT,
  FOREIGN KEY (pedido_idpedido) REFERENCES pedido(idpedido)
);

CREATE TABLE IF NOT EXISTS factura (
  idfactura INTEGER PRIMARY KEY AUTOINCREMENT,
  fecha TEXT,
  iva REAL NOT NULL DEFAULT 0,
  descuento REAL NOT NULL DEFAULT 0,
  total REAL NOT NULL CHECK (total >= 0),
  pedido_idpedido INTEGER,
  FOREIGN KEY (pedido_idpedido) REFERENCES pedido(idpedido)
);

CREATE TABLE IF NOT EXISTS reportes_venta (
  idreportes_venta INTEGER PRIMARY KEY AUTOINCREMENT,
  fecha_inicial TEXT NOT NULL,
  fecha_final TEXT NOT NULL,
  tipo_reporte TEXT NOT NULL,
  total_ventas REAL NOT NULL,
  cantidad_pedidos INTEGER NOT NULL,
  archivo_descargable TEXT,
  analisis_venta TEXT,
  administrador_cedula INTEGER,
  FOREIGN KEY (administrador_cedula) REFERENCES administrador(cedula)
);

CREATE TABLE IF NOT EXISTS pagos (
  idpagos INTEGER PRIMARY KEY AUTOINCREMENT,
  metodo TEXT NOT NULL,
  estado_pago TEXT NOT NULL,
  pedido_idpedido INTEGER,
  FOREIGN KEY (pedido_idpedido) REFERENCES pedido(idpedido)
);

CREATE TABLE IF NOT EXISTS producto_favorito (
  idproducto_favorito INTEGER PRIMARY KEY AUTOINCREMENT,
  fecha_agregado TEXT NOT NULL,
  producto_idproducto INTEGER,
  cliente_cedula INTEGER,
  FOREIGN KEY (producto_idproducto) REFERENCES producto(idproducto),
  FOREIGN KEY (cliente_cedula) REFERENCES cliente(cedula),
  UNIQUE (cliente_cedula, producto_idproducto)
);
