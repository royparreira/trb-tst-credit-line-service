package org.roy.trb.tst.credit.line.services;

import static org.roy.trb.tst.credit.line.enums.FoundingType.SME;
import static org.roy.trb.tst.credit.line.enums.FoundingType.STARTUP;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.roy.trb.tst.credit.line.enums.CreditLineStatus;
import org.roy.trb.tst.credit.line.enums.FoundingType;
import org.roy.trb.tst.credit.line.exception.RejectedCreditLineException;
import org.roy.trb.tst.credit.line.models.CreditLineRequest;
import org.roy.trb.tst.credit.line.models.CreditLineResponse;
import org.roy.trb.tst.credit.line.models.RequesterFinancialData;
import org.roy.trb.tst.credit.line.services.mappers.CreditLineRequestMapper;
import org.roy.trb.tst.credit.line.services.strategies.ICreditLineStrategy;
import org.roy.trb.tst.credit.line.services.strategies.SMECreditLineValidator;
import org.roy.trb.tst.credit.line.services.strategies.StartUpCreditLineValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CreditLineServiceImpl implements ICreditLineService {

  private final CreditLineRequestMapper mapper;

  private ICreditLineStrategy creditLineStrategy;

  @Value("${ratio.monthly-revenue}")
  private Integer monthlyRevenueRatio;

  @Value("${ratio.cash-balance}")
  private Integer cashBalanceRatio;

  /** {@inheritDoc} */
  @Override
  public CreditLineResponse validateCreditLine(
      UUID customerId, CreditLineRequest creditLineRequest, FoundingType foundingType) {

    setCreditLineRequestValidationStrategy(foundingType);

    RequesterFinancialData requesterFinancialData =
        mapper.mapToRequesterFinancialData(creditLineRequest);

    BigDecimal approvedCredit =
        creditLineStrategy
            .getCreditLine(requesterFinancialData)
            .orElseThrow(RejectedCreditLineException::new);

    return CreditLineResponse.builder()
        .creditLineStatus(CreditLineStatus.ACCEPTED)
        .approvedCredit(approvedCredit)
        .build();
  }

  /**
   * Set up the credit line validation logic based on the Strategy design pattern. Select the
   * strategy based on the founding type
   *
   * @param foundingType strategy selector
   */
  private void setCreditLineRequestValidationStrategy(FoundingType foundingType) {

    if (SME.equals(foundingType)) {
      creditLineStrategy =
          SMECreditLineValidator.builder().monthlyRevenueRatio(monthlyRevenueRatio).build();
    }

    if (STARTUP.equals(foundingType)) {
      creditLineStrategy =
          StartUpCreditLineValidator.builder()
              .cashBalanceRatio(cashBalanceRatio)
              .monthlyRevenueRatio(monthlyRevenueRatio)
              .build();
    }
  }
}
