package matypist.openstud.driver.core.internals;

import matypist.openstud.driver.core.OpenstudHelper;
import matypist.openstud.driver.core.models.CertificateType;

import java.util.Map;

public interface ProviderConfig {
    String getEndpointAPI(OpenstudHelper.Mode mode);

    String getEndpointLogin(OpenstudHelper.Mode mode);

    String getEndpointTimetable(OpenstudHelper.Mode mode);

    String getEmailURL();

    boolean isAuthEnabled();

    boolean isClassroomEnabled();

    boolean isExamEnabled();

    boolean isNewsEnabled();

    boolean isTaxEnabled();

    boolean isBioEnabled();

    boolean isSurveyEnabled();

    boolean isCareerForCertificateEnabled();

    boolean isCertEnabled();

    boolean isRefreshEnabled();

    boolean isCertSupported(CertificateType certificate);

    boolean isStudentCardEnabled();

    boolean isStudentPhotoEnabled();

    String getKey(String key);

    void addKeys(Map<String, String> customKeys);

    String getKey(OpenstudHelper.Mode mode);

}
