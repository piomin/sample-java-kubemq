package pl.piomin.service.kubemq.service;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.piomin.service.kubemq.controller.OrderController;
import pl.piomin.service.kubemq.exception.InsufficientFundsException;
import pl.piomin.service.kubemq.model.Account;
import pl.piomin.service.kubemq.model.Order;
import pl.piomin.service.kubemq.model.OrderStatus;
import pl.piomin.service.kubemq.repository.AccountRepository;

@Service
public class OrderProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderProcessor.class);

    private AccountRepository accountRepository;

    public OrderProcessor(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Order process(Order order) {
        try {
            Account from = accountRepository.updateBalance(order.getAccountIdFrom(), -1 * order.getAmount());
            LOGGER.info("Balance updated: {}", from);
            Account to = accountRepository.updateBalance(order.getAccountIdTo(), order.getAmount());
            LOGGER.info("Balance updated: {}", to);
            order.setStatus(OrderStatus.CONFIRMED);
        } catch (InsufficientFundsException e) {
            order.setStatus(OrderStatus.REJECTED);
        }
        return order;
    }

}
