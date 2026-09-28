package com.orderhub.domain;

public enum OrderStatus {
    NEW, PAID, SHIPPED, CANCELLED;

    public void requireTransitionTo(OrderStatus next) {
        boolean allowed = (this == NEW && next == PAID)
                || (this == PAID && next == SHIPPED)
                || (this == SHIPPED && next == CANCELLED);
        if (!allowed) throw new IllegalStateException("Illegal status transition: " + this + " → " + next);
    }
}
