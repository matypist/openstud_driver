package matypist.openstud.driver.core;

import matypist.openstud.driver.exceptions.OpenstudInvalidCredentialsException;

public class OpenstudValidator {
    public static boolean validatePassword(Openstud os) {
        return validatePassword(os, os.getStudentPassword());
    }

    public static boolean validatePassword(Openstud os, String password) {
        // https://regex101.com/r/NZ9X7L/1

        OpenstudHelper.Provider provider = os.getProvider();
        String nice_path;
        if (provider == OpenstudHelper.Provider.SAPIENZA) {
            nice_path = "^(?!.*\\s)" +   // start (no spaces)
                    "(?=[a-zA-Z0-9])" +  // at least one alphanumeric character
                    "(?=.*[A-Z])" +   // at least one uppercase letter
                    "(?=.*[a-z])" +   // at least one lowercase letter
                    "(?=.*[0-9]|.*[!?\\-+*/.,:;_{}\\[\\]()@$%#&=^])" + // at least one digit or special character
                    ".{8,}" + // at least 8 characters
                    "|" + // or
                    "^(?!.*\\s)" +  // start (no spaces)
                    "(?=[a-zA-Z0-9])" +  // at least one alphanumeric character
                    "(?=.*[A-Z])" +   // at least one uppercase letter
                    "(?=.*[a-z]|.*[!?\\-+*/.,:;_{}\\[\\]()@$%#&=^])" +  // at least one lowercase letter or special character
                    "(?=.*[0-9])" +   // at least one digit
                    ".{8,}" + // at least 8 characters
                    "|" + // or
                    "^(?!.*\\s)" +  // start (no spaces)
                    "(?=[a-zA-Z0-9])" +  // at least one alphanumeric character
                    "(?=.*[A-Z]|.*[!?\\-+*/.,:;_{}\\[\\]()@$%#&=^])" +  // at least one uppercase letter or special character
                    "(?=.*[a-z])" +   // at least one lowercase letter
                    "(?=.*[0-9])" +   // at least one digit
                    ".{8,}" + // at least 8 characters
                    "$";   // end
        } else {
            throw new IllegalArgumentException("Password validation not supported for this provider");
        }
        return password != null && password.matches(nice_path);
    }

    //to be used when password is forgotten
    public static boolean validateUserID(Openstud os) throws OpenstudInvalidCredentialsException {
        return os.getStudentID() != null;
    }

    public static boolean validate(Openstud os) throws OpenstudInvalidCredentialsException {
        return validatePassword(os) && validateUserID(os);
    }
}
