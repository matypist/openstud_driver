package matypist.openstud.driver.core.internals;

import matypist.openstud.driver.core.models.Isee;
import matypist.openstud.driver.core.models.Tax;
import matypist.openstud.driver.exceptions.OpenstudConnectionException;
import matypist.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import matypist.openstud.driver.exceptions.OpenstudInvalidResponseException;

import java.util.List;

public interface TaxHandler {
    List<Tax> getUnpaidTaxes() throws OpenstudConnectionException, OpenstudInvalidResponseException,
            OpenstudInvalidCredentialsException;

    List<Tax> getPaidTaxes() throws OpenstudConnectionException, OpenstudInvalidResponseException,
            OpenstudInvalidCredentialsException;

    byte[] getPaymentSlipPDF(Tax unpaidTax) throws OpenstudConnectionException, OpenstudInvalidResponseException,
            OpenstudInvalidCredentialsException;

    Isee getCurrentIsee() throws OpenstudConnectionException, OpenstudInvalidResponseException,
            OpenstudInvalidCredentialsException;

    List<Isee> getIseeHistory() throws OpenstudConnectionException, OpenstudInvalidResponseException,
            OpenstudInvalidCredentialsException;


}
