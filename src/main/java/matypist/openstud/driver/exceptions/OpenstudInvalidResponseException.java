package matypist.openstud.driver.exceptions;

public class OpenstudInvalidResponseException extends OpenstudBaseResponseException {
    public OpenstudInvalidResponseException(String message) {
        super(message);
    }

    public OpenstudInvalidResponseException(Exception e) {
        super(e);
    }

    @Override
    public OpenstudInvalidResponseException setJSONType() {
        super.setJSONType();
        return this;
    }

    public OpenstudInvalidResponseException setMaintenanceType() {
        super.setMaintenanceType();
        return this;
    }

    public OpenstudInvalidResponseException setRateLimitType() {
        super.setRateLimitType();
        return this;
    }

    public OpenstudInvalidResponseException setHTMLType() {
        super.setHTMLType();
        return this;
    }

    public OpenstudInvalidResponseException setSSLType() {
        super.setSSLType();
        return this;
    }

}
