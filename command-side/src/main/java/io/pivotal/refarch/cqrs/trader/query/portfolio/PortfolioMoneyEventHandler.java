/*
 * Copyright (c) 2010-2012. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pivotal.refarch.cqrs.trader.query.portfolio;

import io.pivotal.refarch.cqrs.trader.coreapi.portfolio.PortfolioCreatedEvent;
import io.pivotal.refarch.cqrs.trader.coreapi.portfolio.cash.CashDepositedEvent;
import io.pivotal.refarch.cqrs.trader.coreapi.portfolio.cash.CashReservationCancelledEvent;
import io.pivotal.refarch.cqrs.trader.coreapi.portfolio.cash.CashReservationConfirmedEvent;
import io.pivotal.refarch.cqrs.trader.coreapi.portfolio.cash.CashReservedEvent;
import io.pivotal.refarch.cqrs.trader.coreapi.portfolio.cash.CashWithdrawnEvent;
import io.pivotal.refarch.cqrs.trader.query.user.UserViewRepository;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@ProcessingGroup("queryModel")
public class PortfolioMoneyEventHandler {

    private final static Logger logger = LoggerFactory.getLogger(PortfolioMoneyEventHandler.class);

    private final PortfolioViewRepository portfolioViewRepository;
    private final UserViewRepository userViewRepository;

    public PortfolioMoneyEventHandler(PortfolioViewRepository portfolioViewRepository,
                                      UserViewRepository userViewRepository) {
        this.portfolioViewRepository = portfolioViewRepository;
        this.userViewRepository = userViewRepository;
    }

    @EventHandler
    public void on(PortfolioCreatedEvent event) {
        String userIdString = event.getUserId().toString();
        logger.debug("About to handle the PortfolioCreatedEvent for user with identifier {}", userIdString);

        PortfolioView portfolioView = new PortfolioView();
        portfolioView.setIdentifier(event.getPortfolioId().toString());
        portfolioView.setUserIdentifier(userIdString);
        portfolioView.setUserName(userViewRepository.findByIdentifier(userIdString).getFullName());
        portfolioView.setAmountOfMoney(0);
        portfolioView.setReservedAmountOfMoney(0);

        portfolioViewRepository.save(portfolioView);
    }

    @EventHandler
    public void on(CashDepositedEvent event) {
        PortfolioView portfolioView = portfolioViewRepository.findById(event.getPortfolioId().toString()).orElse(null);
        portfolioView.setAmountOfMoney(portfolioView.getAmountOfMoney() + event.getMoneyAddedInCents());

        portfolioViewRepository.save(portfolioView);
    }

    @EventHandler
    public void on(CashWithdrawnEvent event) {
        PortfolioView portfolioView = portfolioViewRepository.findById(event.getPortfolioId().toString()).orElse(null);

        portfolioView.setAmountOfMoney(portfolioView.getAmountOfMoney() - event.getAmountPaidInCents());

        portfolioViewRepository.save(portfolioView);
    }

    @EventHandler
    public void on(CashReservedEvent event) {
        PortfolioView portfolioView = portfolioViewRepository.findById(event.getPortfolioId().toString()).orElse(null);

        portfolioView.setReservedAmountOfMoney(portfolioView.getReservedAmountOfMoney() + event.getAmountToReserve());

        portfolioViewRepository.save(portfolioView);
    }

    @EventHandler
    public void on(CashReservationCancelledEvent event) {
        PortfolioView portfolioView = portfolioViewRepository.findById(event.getPortfolioId().toString()).orElse(null);

        portfolioView.setReservedAmountOfMoney(
                portfolioView.getReservedAmountOfMoney() - event.getAmountOfMoneyToCancel()
        );

        portfolioViewRepository.save(portfolioView);
    }

    @EventHandler
    public void on(CashReservationConfirmedEvent event) {
        PortfolioView portfolioView = portfolioViewRepository.getOne(event.getPortfolioId().toString());

        long reservedAmountOfMoney = portfolioView.getReservedAmountOfMoney();
        long amountOfMoneyConfirmed = event.getAmountOfMoneyConfirmedInCents();
        if (amountOfMoneyConfirmed < reservedAmountOfMoney) {
            portfolioView.setReservedAmountOfMoney(reservedAmountOfMoney - amountOfMoneyConfirmed);
        } else {
            portfolioView.setReservedAmountOfMoney(0);
        }
        portfolioView.setAmountOfMoney(portfolioView.getAmountOfMoney() - amountOfMoneyConfirmed);

        portfolioViewRepository.save(portfolioView);
    }
}
