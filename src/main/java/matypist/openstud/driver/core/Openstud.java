package matypist.openstud.driver.core;

import matypist.openstud.driver.core.internals.*;
import matypist.openstud.driver.core.models.*;
import matypist.openstud.driver.core.providers.sapienza.*;
import matypist.openstud.driver.exceptions.*;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.tuple.Pair;
import org.threeten.bp.LocalDate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Openstud implements AuthenticationHandler, BioHandler, NewsHandler, TaxHandler, ClassroomHandler, ExamHandler {
    private int maxTries;
    private String endpointAPI;
    private String endpointLogin;
    private String endpointTimetable;
    private volatile String token;
    private String studentPassword;
    private String studentID;
    private boolean isReady;
    private Logger logger;
    private OkHttpClient client;
    private String key;
    private int waitTimeClassroomRequest;
    private int limitSearch;
    private OpenstudHelper.Provider provider;
    private AuthenticationHandler authenticator;
    private BioHandler personal;
    private NewsHandler newsHandler;
    private TaxHandler taxHandler;
    private ClassroomHandler classroomHandler;
    private ExamHandler examHandler;
    private ProviderConfig config;
    private OpenstudHelper.Mode mode;

    public Openstud() {
        super();
    }

    Openstud(OpenstudBuilder builder) {
        this.provider = builder.provider;
        this.maxTries = builder.retryCounter;
        this.studentID = builder.studentID;
        this.studentPassword = builder.password;
        this.logger = builder.logger;
        this.isReady = builder.readyState;
        this.waitTimeClassroomRequest = builder.waitTimeClassroomRequest;
        this.limitSearch = builder.limitSearchResults;
        this.mode = builder.mode;

        Pair<SSLSocketFactory, X509TrustManager> sslComponents = setupCustomSSL();
        SSLSocketFactory sslSocketFactory = sslComponents.getLeft();
        X509TrustManager trustManager = sslComponents.getRight();

        // Build the OkHttpClient
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(builder.connectTimeout, TimeUnit.SECONDS)
                .writeTimeout(builder.writeTimeout, TimeUnit.SECONDS)
                .readTimeout(builder.readTimeout, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionSpecs(Collections.singletonList(ConnectionSpec.COMPATIBLE_TLS));

        // Apply custom SSLSocketFactory only if successfully created
        if (sslSocketFactory != null && trustManager != null) {
            clientBuilder.sslSocketFactory(sslSocketFactory, trustManager);
        }

        client = clientBuilder.build();

        init();
        config.addKeys(builder.keyMap);
    }

    /**
     * Loads a single PEM certificate from resources into the provided KeyStore.
     * @param keyStore The KeyStore to add the certificate to.
     * @param pemResourcePath The resource path of the PEM file (e.g., "certs/sapienza/uniroma1.pem").
     * @param alias The alias to assign to the certificate entry.
     * @return true if the certificate was found and loaded, false otherwise.
     * @throws Exception If certificate parsing or KeyStore operations fail.
     */
    private boolean loadCertificate(KeyStore keyStore, String pemResourcePath, String alias) throws Exception {
        InputStream pemInputStream = getClass().getClassLoader().getResourceAsStream(pemResourcePath);
        if (pemInputStream == null) {
            log(Level.WARNING, "Custom certificate '" + pemResourcePath + "' not found in resources. Skipping.");
            return false; // Certificate not found
        }

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(pemInputStream);
            keyStore.setCertificateEntry(alias, cert);
            log(Level.FINE, "Loaded custom certificate '" + pemResourcePath + "' with alias '" + alias + "'.");
            return true; // Certificate loaded
        } finally {
            if (pemInputStream != null) {
                pemInputStream.close();
            }
        }
    }

    /**
     * Loads all certificates from a given resource directory into the KeyStore.
     * This method is designed to work both from a filesystem (IDE) and a JAR file.
     * @param keyStore The KeyStore to add the certificates to.
     * @param path The resource path of the directory (e.g., "certs/sapienza").
     * @return true if at least one certificate was loaded, false otherwise.
     */
    private boolean loadCertificatesFromResourceDirectory(KeyStore keyStore, String path) {
        boolean loadedAny = false;
        log(Level.FINE, "Loading custom certificates from resource directory: " + path);

        try {
            URL dirURL = getClass().getClassLoader().getResource(path);

            if (dirURL == null) {
                log(Level.WARNING, "Resource directory '" + path + "' not found. Skipping custom certificate loading.");
                return false;
            }

            if (dirURL.getProtocol().equals("file")) {
                // Running from filesystem (e.g., IDE)
                File certDir = new File(dirURL.toURI());
                File[] files = certDir.listFiles();
                if (files == null) {
                    log(Level.WARNING, "Could not list files in directory: " + certDir.getAbsolutePath());
                    return false;
                }
                int aliasCounter = 0;
                for (File file : files) {
                    if (file.isFile()) {
                        String resourcePath = path + "/" + file.getName();
                        String alias = "custom-cert-" + (aliasCounter++);
                        if (loadCertificate(keyStore, resourcePath, alias)) {
                            loadedAny = true;
                        }
                    }
                }
            } else if (dirURL.getProtocol().equals("jar")) {
                // Running from JAR
                JarURLConnection jarURLConnection = (JarURLConnection) dirURL.openConnection();
                JarFile jarFile = jarURLConnection.getJarFile();
                java.util.Enumeration<JarEntry> entries = jarFile.entries();

                String dirPath = path + "/";
                int aliasCounter = 0;

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (entryName.startsWith(dirPath) && !entry.isDirectory()) {
                        String alias = "custom-cert-" + (aliasCounter++);
                        if (loadCertificate(keyStore, entryName, alias)) {
                            loadedAny = true;
                        }
                    }
                }
            } else {
                log(Level.WARNING, "Unsupported protocol '" + dirURL.getProtocol() + "' for resource directory. Skipping custom certificates.");
                return false;
            }
        } catch (Exception e) {
            // Catch all exceptions related to cert loading (IO, Security, URI, etc.)
            log(Level.SEVERE, "Failed to load certificates from directory '" + path + "'. Error: " + e.getMessage());
            return false;
        }

        return loadedAny;
    }


    /**
     * Sets up a custom SSLContext and TrustManager.
     * If the provider is SAPIENZA, this attempts to load all certificates
     * from the 'certs/sapienza' resource directory and adds them to the default system trust store.
     * If the provider is not SAPIENZA, or if the custom certs are not found,
     * it returns nulls, forcing OkHttp to use the default system TrustManager.
     * @return A Pair containing the SSLSocketFactory (Left) and X509TrustManager (Right),
     * or Pair.of(null, null) if setup fails or is not required.
     */
    private Pair<SSLSocketFactory, X509TrustManager> setupCustomSSL() {
        // Only apply custom certificates for the SAPIENZA provider
        if (this.provider != OpenstudHelper.Provider.SAPIENZA) {
            log(Level.FINE, "Provider is not Sapienza. Using default system TrustManager.");
            return Pair.of(null, null);
        }

        log(Level.FINE, "Provider is Sapienza. Attempting to add custom certificates to default TrustManager.");

        SSLSocketFactory sslSocketFactory = null;
        X509TrustManager trustManager = null;

        try {
            // Create a KeyStore
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null); // Init empty

            // Get the default system TrustManager
            TrustManagerFactory tmfDefault = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmfDefault.init((KeyStore) null); // Initialize with default system KeyStore

            X509TrustManager defaultTrustManager = null;
            for (TrustManager tm : tmfDefault.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    defaultTrustManager = (X509TrustManager) tm;
                    break;
                }
            }
            if (defaultTrustManager == null) {
                throw new IllegalStateException("No default X509TrustManager found");
            }

            // Add all default CAs to our new KeyStore
            int defaultCaCount = 0;
            for (X509Certificate cert : defaultTrustManager.getAcceptedIssuers()) {
                // Use a unique alias for each default CA
                keyStore.setCertificateEntry("default-ca-" + defaultCaCount++, cert);
            }
            log(Level.FINE, "Loaded " + defaultCaCount + " default system CAs.");

            // Add custom SAPIENZA certificates from the resource directory
            boolean customCertLoaded = loadCertificatesFromResourceDirectory(keyStore, "certs/sapienza");

            // If no custom certificates were loaded, just use the default TrustManager
            if (!customCertLoaded) {
                log(Level.WARNING, "Sapienza provider selected, but no custom certificates found in resource directory 'certs/sapienza'. Using default TrustManager only.");
                return Pair.of(null, null);
            }

            // Create a new TrustManagerFactory that trusts both default and custom CAs
            TrustManagerFactory tmfCombined = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmfCombined.init(keyStore);

            // Find the X509TrustManager
            for (TrustManager tm : tmfCombined.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    trustManager = (X509TrustManager) tm;
                    break;
                }
            }
            if (trustManager == null) {
                throw new IllegalStateException("No X509TrustManager found in combined KeyStore");
            }

            // Create an SSLContext that uses our combined TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, null);

            sslSocketFactory = sslContext.getSocketFactory();
            log(Level.FINE, "Custom SSLContext initialized successfully, combining default CAs with custom certificates.");

        } catch (Exception e) {
            log(Level.SEVERE, "Failed to initialize custom SSLContext, falling back to default. Error: " + e.getMessage());
            // Reset to null on failure to ensure default is used
            return Pair.of(null, null);
        }

        return Pair.of(sslSocketFactory, trustManager);
    }


    private void init() {
        if (provider == null) throw new IllegalArgumentException("Provider can't be left null");
        else if (provider == OpenstudHelper.Provider.SAPIENZA) {
            authenticator = new SapienzaAuthenticationHandler(this);
            personal = new SapienzaBioHandler(this);
            newsHandler = new SapienzaNewsHandler(this);
            taxHandler = new SapienzaTaxHandler(this);
            classroomHandler = new SapienzaClassroomHandler(this);
            examHandler = new SapienzaExamHandler(this);
            config = new SapienzaConfig();
        }
        endpointAPI = config.getEndpointAPI(mode);
        endpointLogin = config.getEndpointLogin(mode);
        endpointTimetable = config.getEndpointTimetable(mode);
        key = config.getKey(mode);
    }

    public ProviderConfig getConfig() {
        return config;
    }

    public OpenstudHelper.Provider getProvider(){
        return provider;
    }

    public String getStudentID() {
        return studentID;
    }

    public String getEndpointAPI() {
        return endpointAPI;
    }

    public String getEndpointLogin() {
        return endpointLogin;
    }

    public String getEndpointTimetable() {
        return endpointTimetable;
    }

    public String getStudentPassword() {
        return studentPassword;
    }

    public Logger getLogger() {
        return logger;
    }

    public OkHttpClient getClient() {
        return client;
    }

    public String getKey() {
        return key;
    }

    public String getKey(String keyName) {
        return config.getKey(keyName);
    }

    public int getWaitTimeClassroomRequest() {
        return waitTimeClassroomRequest;
    }

    public int getLimitSearch() {
        return limitSearch;
    }

    public void setStudentPassword(String password) {
        studentPassword = password;
    }

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    public int getMaxTries() {
        return maxTries;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public synchronized String getToken() {
        return this.token;
    }

    void log(Level lvl, String str) {
        if (logger != null) logger.log(lvl, str);
    }

    public void log(Level lvl, Object obj) {
        if (logger != null) logger.log(lvl, obj.toString());
    }

    public boolean isReady() {
        return isReady;
    }

    @Override
    public synchronized void refreshToken() throws OpenstudRefreshException, OpenstudInvalidResponseException {
        if (!config.isRefreshEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        authenticator.refreshToken();
    }

    @Override
    public void login() throws OpenstudInvalidCredentialsException, OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudUserNotEnabledException {
        if (!config.isAuthEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        authenticator.login();
    }

    @Override
    public String getSecurityQuestion() throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isAuthEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return authenticator.getSecurityQuestion();
    }

    @Override
    public boolean recoverPassword(String answer) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException, OpenstudInvalidAnswerException {
        if (!config.isAuthEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return authenticator.recoverPassword(answer);
    }

    @Override
    public void resetPassword(String new_password) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isAuthEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        authenticator.resetPassword(new_password);
    }

    @Override
    public boolean recoverPasswordWithEmail(String email, String answer) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException, OpenstudInvalidAnswerException {
        if (!config.isAuthEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return authenticator.recoverPasswordWithEmail(email, answer);
    }

    @Override
    public Student getInfoStudent() throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isBioEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return personal.getInfoStudent();
    }

    @Override
    public List<Career> getCareersChoicesForCertificate(Student student, CertificateType certificate) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isCareerForCertificateEnabled() || !config.isCertSupported(certificate))
            throw new IllegalStateException("Provider doesn't support this feature");
        return personal.getCareersChoicesForCertificate(student, certificate);
    }

    @Override
    public byte[] getCertificatePDF(Student student, Career career, CertificateType certificate) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isCertEnabled() || !config.isCertSupported(certificate))
            throw new IllegalStateException("Provider doesn't support this feature");
        return personal.getCertificatePDF(student, career, certificate);
    }

    @Override
    public List<News> getNews(String locale, boolean withDescription, Integer limit, Integer page, Integer maxPage,
                              String query) throws OpenstudInvalidResponseException, OpenstudConnectionException {
        if (!config.isNewsEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return newsHandler.getNews(locale, withDescription, limit, page, maxPage, query);
    }

    @Override
    public List<Event> getNewsletterEvents() throws OpenstudInvalidResponseException, OpenstudConnectionException {
        if (!config.isNewsEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return newsHandler.getNewsletterEvents();
    }

    @Override
    public List<Tax> getUnpaidTaxes() throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isTaxEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return taxHandler.getUnpaidTaxes();
    }

    @Override
    public List<Tax> getPaidTaxes() throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isTaxEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return taxHandler.getPaidTaxes();
    }

    @Override
    public byte[] getPaymentSlipPDF(Tax unpaidTax) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isTaxEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        if (unpaidTax.getStatus() == Tax.TaxStatus.PAID) throw new IllegalStateException("Provider doesn't support printing of paid slips");
        return taxHandler.getPaymentSlipPDF(unpaidTax);
    }

    @Override
    public Isee getCurrentIsee() throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isTaxEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return taxHandler.getCurrentIsee();
    }

    @Override
    public List<Isee> getIseeHistory() throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isTaxEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return taxHandler.getIseeHistory();
    }

    @Override
    public List<Classroom> getClassRoom(String query, boolean withTimetable) throws OpenstudInvalidResponseException, OpenstudConnectionException {
        if (!config.isClassroomEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return classroomHandler.getClassRoom(query, withTimetable);
    }

    @Override
    public List<Lesson> getClassroomTimetable(Classroom room, LocalDate date) throws OpenstudConnectionException, OpenstudInvalidResponseException {
        if (!config.isClassroomEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return classroomHandler.getClassroomTimetable(room, date);
    }

    @Override
    public List<Lesson> getClassroomTimetable(int id, LocalDate date) throws OpenstudConnectionException, OpenstudInvalidResponseException {
        if (!config.isClassroomEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return classroomHandler.getClassroomTimetable(id, date);
    }

    @Override
    public Map<String, List<Lesson>> getTimetable(List<ExamDoable> exams) throws OpenstudInvalidResponseException, OpenstudConnectionException {
        if (!config.isClassroomEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return classroomHandler.getTimetable(exams);
    }

    @Override
    public List<ExamDoable> getExamsDoable() throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isExamEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return examHandler.getExamsDoable();
    }

    @Override
    public List<ExamDone> getExamsDone() throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isExamEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return examHandler.getExamsDone();
    }

    @Override
    public String getCourseSurvey(String surveyCode) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isSurveyEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return examHandler.getCourseSurvey(surveyCode);
    }

    @Override
    public List<ExamReservation> getActiveReservations() throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isExamEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return examHandler.getActiveReservations();
    }

    @Override
    public List<ExamReservation> getAvailableReservations(ExamDoable exam, Student student) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isExamEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return examHandler.getAvailableReservations(exam, student);
    }

    @Override
    public Pair<Integer, String> insertReservation(ExamReservation res) throws OpenstudInvalidResponseException, OpenstudConnectionException, OpenstudInvalidCredentialsException {
        if (!config.isExamEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return examHandler.insertReservation(res);
    }

    @Override
    public int deleteReservation(ExamReservation res) throws OpenstudInvalidResponseException, OpenstudConnectionException, OpenstudInvalidCredentialsException {
        if (!config.isExamEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return examHandler.deleteReservation(res);
    }

    @Override
    public byte[] getExamReservationPDF(ExamReservation reservation) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isExamEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return examHandler.getExamReservationPDF(reservation);
    }

    @Override
    public List<Event> getCalendarEvents(Student student) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isExamEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return examHandler.getCalendarEvents(student);
    }

    @Override
    public byte[] getStudentPhoto(Student student) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isStudentPhotoEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return personal.getStudentPhoto(student);
    }

    @Override
    public StudentCard getStudentCard(Student student, boolean withPhoto) throws OpenstudConnectionException, OpenstudInvalidResponseException, OpenstudInvalidCredentialsException {
        if (!config.isStudentCardEnabled()) throw new IllegalStateException("Provider doesn't support this feature");
        return personal.getStudentCard(student, withPhoto);
    }
}