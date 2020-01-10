package pl.piomin.service.kubemq.service;

import org.springframework.stereotype.Service;
import pl.piomin.service.kubemq.exception.InsufficientFundsException;
import pl.piomin.service.kubemq.model.Order;
import pl.piomin.service.kubemq.model.OrderStatus;
import pl.piomin.service.kubemq.repository.AccountRepository;

@Service
public class OrderProcessor {

    private AccountRepository accountRepository;

    public OrderProcessor(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Order process(Order order) {
        try {
            accountRepository.updateBalance(order.getAccountIdFrom(), -1 * order.getAmount());
            accountRepository.updateBalance(order.getAccountIdTo(), order.getAmount());
            order.setStatus(OrderStatus.CONFIRMED);
        } catch (InsufficientFundsException e) {
            order.setStatus(OrderStatus.REJECTED);
        }
        return order;
    }

}
