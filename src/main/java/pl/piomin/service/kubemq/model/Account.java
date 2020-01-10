package pl.piomin.service.kubemq.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class Account {

    private Integer id;
    private String number;
    @EqualsAndHashCode.Exclude private int balance;

}
