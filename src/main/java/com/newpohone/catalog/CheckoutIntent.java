package com.newpohone.catalog;

import java.io.Serializable;
import java.util.Map;

public class CheckoutIntent implements Serializable {

    public static final String SESSION_KEY = "checkoutIntent";

    private String metodo;
    private String nombre;
    private String telefono;
    private String direccion;
    private Map<String, Object> cart;
    private long createdAt;

    public String getMetodo() {
        return metodo;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public Map<String, Object> getCart() {
        return cart;
    }

    public void setCart(Map<String, Object> cart) {
        this.cart = cart;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean expired() {
        return System.currentTimeMillis() - createdAt > 15 * 60 * 1000;
    }
}
