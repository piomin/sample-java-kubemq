package pl.piomin.service.kubemq.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Order implements Serializable {

    private OrderType type;
    private Integer accountIdFrom;
    private Integer accountIdTo;
    private LocalDateTime date;
    private int amount;
    private String id;
    private OrderStatus status;

    public Order() {
    }

    public Order(OrderType type, Integer accountIdFrom, Integer accountIdTo, LocalDateTime date, int amount, String id, OrderStatus status) {
        this.type = type;
        this.accountIdFrom = accountIdFrom;
        this.accountIdTo = accountIdTo;
        this.date = date;
        this.amount = amount;
        this.id = id;
        this.status = status;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public Integer getAccountIdFrom() {
        return accountIdFrom;
    }

    public void setAccountIdFrom(Integer accountIdFrom) {
        this.accountIdFrom = accountIdFrom;
    }

    public Integer getAccountIdTo() {
        return accountIdTo;
    }

    public void setAccountIdTo(Integer accountIdTo) {
        this.accountIdTo = accountIdTo;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Order{" +
                "type=" + type +
                ", accountIdFrom=" + accountIdFrom +
                ", accountIdTo=" + accountIdTo +
                ", date=" + date +
                ", amount=" + amount +
                ", id='" + id + '\'' +
                ", status=" + status +
                '}';
    }
}
