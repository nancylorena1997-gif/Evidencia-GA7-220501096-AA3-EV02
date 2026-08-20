package com.newpohone.module;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

import static com.newpohone.module.ModuleDefinition.FieldDefinition;

@Component
public class ModuleRegistry {

    private final Map<String, ModuleDefinition> modules = new LinkedHashMap<>();

    public ModuleRegistry() {
        register(new ModuleDefinition("cuentas", "Cuentas", "cuenta", "Usuarios", "cuenta",
                "idcuenta", true, "bi-person-badge",
                id("idcuenta", "ID"),
                req("email", "Correo electrónico", "email"),
                req("contrasena", "Contraseña", "password")));

        register(new ModuleDefinition("clientes", "Clientes", "cliente", "Usuarios", "cliente",
                "cedula", false, "bi-people",
                requiredId("cedula", "Cédula"),
                req("nombre", "Nombre completo", "text"),
                req("telefono", "Teléfono", "tel"),
                req("registro", "Fecha de registro", "date"),
                field("cuenta_idcuenta", "Cuenta", "number")));

        register(new ModuleDefinition("administradores", "Administradores", "administrador",
                "Usuarios", "administrador", "cedula", false, "bi-shield-lock",
                requiredId("cedula", "Cédula"),
                req("nombre", "Nombre completo", "text"),
                req("permisos", "Permisos", "text"),
                req("telefono", "Teléfono", "tel"),
                field("cuenta_idcuenta", "Cuenta", "number")));

        register(new ModuleDefinition("categorias", "Categorías", "categoría", "Catálogo",
                "categoria", "id_categoria", true, "bi-tags",
                id("id_categoria", "ID"),
                req("nombre", "Nombre", "text")));

        register(new ModuleDefinition("productos", "Productos", "producto", "Catálogo", "producto",
                "idproducto", true, "bi-phone",
                id("idproducto", "ID"),
                req("nombre", "Nombre", "text"),
                req("precio", "Precio", "decimal"),
                req("stock", "Unidades disponibles", "number"),
                req("descripcion", "Descripción", "textarea"),
                field("administrador_cedula", "Administrador", "number"),
                field("categoria_id_categoria", "Categoría", "number")));

        register(new ModuleDefinition("carritos", "Carritos", "carrito", "Compras", "carrito",
                "idcarrito", true, "bi-cart3",
                id("idcarrito", "ID"),
                req("fecha", "Fecha", "date"),
                field("cliente_cedula", "Cliente", "number")));

        register(new ModuleDefinition("detalles-carrito", "Detalle de carritos", "detalle",
                "Compras", "detalle_carrito", "iddetalle_carrito", true, "bi-cart-plus",
                id("iddetalle_carrito", "ID"),
                req("subtotal", "Subtotal", "decimal"),
                req("cantidad", "Cantidad", "number"),
                req("precio_unitario", "Precio unitario", "decimal"),
                field("producto_idproducto", "Producto", "number"),
                field("carrito_idcarrito", "Carrito", "number")));

        register(new ModuleDefinition("favoritos", "Productos favoritos", "favorito", "Compras",
                "producto_favorito", "idproducto_favorito", true, "bi-heart",
                id("idproducto_favorito", "ID"),
                req("fecha_agregado", "Fecha", "date"),
                field("producto_idproducto", "Producto", "number"),
                field("cliente_cedula", "Cliente", "number")));

        register(new ModuleDefinition("pedidos", "Pedidos", "pedido", "Pedidos", "pedido",
                "idpedido", true, "bi-bag-check",
                id("idpedido", "ID"),
                req("fecha", "Fecha", "date"),
                req("estado", "Estado", "text"),
                req("total", "Total", "decimal"),
                field("cliente_cedula", "Cliente", "number")));

        register(new ModuleDefinition("detalles-pedido", "Detalle de pedidos", "detalle", "Pedidos",
                "detalle_pedido", "iddetalle_pedido", true, "bi-list-ul",
                id("iddetalle_pedido", "ID"),
                req("cantidad", "Cantidad", "number"),
                req("subtotal", "Subtotal", "decimal"),
                field("pedido_idpedido", "Pedido", "number"),
                field("producto_idproducto", "Producto", "number")));

        register(new ModuleDefinition("envios", "Envíos", "envío", "Pedidos", "envio",
                "id_envio", true, "bi-truck",
                id("id_envio", "ID"),
                req("direccion", "Dirección", "textarea"),
                req("fecha_envio", "Fecha de envío", "date"),
                req("estado_envio", "Estado", "text"),
                field("guia", "Guía de seguimiento", "text"),
                field("telefono", "Teléfono", "tel"),
                field("destinatario", "Destinatario", "text"),
                field("pedido_idpedido", "Pedido", "number")));

        register(new ModuleDefinition("facturas", "Facturas", "factura", "Pedidos", "factura",
                "idfactura", true, "bi-receipt",
                id("idfactura", "ID"),
                field("fecha", "Fecha", "date"),
                req("iva", "IVA", "decimal"),
                req("descuento", "Descuento", "decimal"),
                req("total", "Total", "decimal"),
                field("pedido_idpedido", "Pedido", "number")));

        register(new ModuleDefinition("pagos", "Pagos", "pago", "Pedidos", "pagos",
                "idpagos", true, "bi-credit-card",
                id("idpagos", "ID"),
                req("metodo", "Método", "text"),
                req("estado_pago", "Estado", "text"),
                field("pedido_idpedido", "Pedido", "number")));

        register(new ModuleDefinition("resenas", "Reseñas", "reseña", "Atención", "resena",
                "id_resena", true, "bi-star",
                id("id_resena", "ID"),
                req("comentario", "Comentario", "textarea"),
                req("calificacion", "Calificación (1 a 5)", "number"),
                field("cliente_cedula", "Cliente", "number"),
                field("producto_idproducto", "Producto", "number")));

        register(new ModuleDefinition("tickets", "Atención al cliente", "ticket", "Atención",
                "atencion_cliente", "id_ticket", true, "bi-headset",
                id("id_ticket", "ID"),
                req("mensaje", "Mensaje", "textarea"),
                req("fecha", "Fecha", "date"),
                field("respuesta", "Respuesta", "textarea"),
                field("cliente_cedula", "Cliente", "number")));

        register(new ModuleDefinition("reportes", "Reportes de venta", "reporte", "Reportes",
                "reportes_venta", "idreportes_venta", true, "bi-graph-up-arrow",
                id("idreportes_venta", "ID"),
                req("fecha_inicial", "Fecha inicial", "date"),
                req("fecha_final", "Fecha final", "date"),
                req("tipo_reporte", "Tipo", "text"),
                req("total_ventas", "Total de ventas", "decimal"),
                req("cantidad_pedidos", "Cantidad de pedidos", "number"),
                field("archivo_descargable", "Archivo", "text"),
                field("analisis_venta", "Análisis", "textarea"),
                field("administrador_cedula", "Administrador", "number")));
    }

    public ModuleDefinition get(String key) {
        return modules.get(key);
    }

    public Collection<ModuleDefinition> all() {
        return modules.values();
    }

    private void register(ModuleDefinition module) {
        modules.put(module.getKey(), module);
    }

    private static FieldDefinition id(String name, String label) {
        return new FieldDefinition(name, label, "number", false, true);
    }

    private static FieldDefinition requiredId(String name, String label) {
        return new FieldDefinition(name, label, "number", true, false);
    }

    private static FieldDefinition req(String name, String label, String type) {
        return new FieldDefinition(name, label, type, true, false);
    }

    private static FieldDefinition field(String name, String label, String type) {
        return new FieldDefinition(name, label, type, false, false);
    }
}
