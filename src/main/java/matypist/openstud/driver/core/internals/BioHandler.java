package matypist.openstud.driver.core.internals;

import matypist.openstud.driver.core.models.Career;
import matypist.openstud.driver.core.models.CertificateType;
import matypist.openstud.driver.core.models.Student;
import matypist.openstud.driver.core.models.StudentCard;
import matypist.openstud.driver.exceptions.OpenstudConnectionException;
import matypist.openstud.driver.exceptions.OpenstudInvalidCredentialsException;
import matypist.openstud.driver.exceptions.OpenstudInvalidResponseException;

import java.util.List;


public interface BioHandler {
    Student getInfoStudent() throws OpenstudConnectionException, OpenstudInvalidResponseException,
            OpenstudInvalidCredentialsException;

    List<Career> getCareersChoicesForCertificate(Student student, CertificateType certificate) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException;

    byte[] getCertificatePDF(Student student, Career career, CertificateType certificate) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException;


    byte[] getStudentPhoto(Student student) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException;

    StudentCard getStudentCard(Student student, boolean withPhoto) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException;
}