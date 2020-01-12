package pl.piomin.service.kubemq.repository;

import org.springframework.stereotype.Repository;
import pl.piomin.service.kubemq.exception.InsufficientFundsException;
import pl.piomin.service.kubemq.model.Account;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

@Repository
public class AccountRepository {

    private List<Account> accounts = new ArrayList<>();

    public Account updateBalance(Integer id, int amount) throws InsufficientFundsException {
        Optional<Account> accOptional = accounts.stream().filter(a -> a.getId().equals(id)).findFirst();
        if (accOptional.isPresent()) {
            Account account = accOptional.get();
            account.setBalance(account.getBalance() + amount);
            if (account.getBalance() < 0)
                throw new InsufficientFundsException();
            int index = accounts.indexOf(account);
            accounts.set(index, account);
            return account;
        }
        return null;
    }

    public Account add(Account account) {
        account.setId(accounts.size() + 1);
        accounts.add(account);
        return account;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    @PostConstruct
    public void init() {
        add(new Account(null, "123456", 2000));
        add(new Account(null, "123457", 2000));
        add(new Account(null, "123458", 2000));
    }
}
