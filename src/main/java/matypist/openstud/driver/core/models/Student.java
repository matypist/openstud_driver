package matypist.openstud.driver.core.models;

import org.threeten.bp.LocalDate;

import java.util.Objects;

public class Student {
    private String CF;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String birthCity;
    private String birthPlace;
    private String courseYear;
    private String firstEnrollment;
    private String lastEnrollment;
    private String departmentName;
    private String courseName;
    private String nation;
    private String email;
    private String citizenship;
    private String gender;
    private String studentStatus;
    private int academicYear;
    private int academicYearCourse;
    private String studentID;
    private int codeCourse;
    private int typeStudent;
    private int cfu;
    private boolean isErasmus;
    private String userUUID;
    private String nameAlias;
    private String surnameAlias;
    private String personalEmail;

    public String getStudentStatus() {
        return studentStatus;
    }

    public void setStudentStatus(String studentStatus) {
        this.studentStatus = studentStatus;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getCitizenship() {
        return citizenship;
    }

    public void setCitizenship(String citizenship) {
        this.citizenship = citizenship;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getCfu() {
        return cfu;
    }

    public void setCfu(int cfu) {
        this.cfu = cfu;
    }

    public String getSocialSecurityNumber() {
        return CF;
    }

    public void setSocialSecurityNumber(String CF) {
        this.CF = CF;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getBirthCity() {
        return birthCity;
    }

    public void setBirthCity(String birthCity) {
        this.birthCity = birthCity;
    }

    public String getCourseYear() {
        return courseYear;
    }

    public void setCourseYear(String courseYear) {
        this.courseYear = courseYear;
    }

    public String getFirstEnrollment() {
        return firstEnrollment;
    }

    public void setFirstEnrollment(String firstEnrollment) {
        this.firstEnrollment = firstEnrollment;
    }

    public String getLastEnrollment() {
        return lastEnrollment;
    }

    public void setLastEnrollment(String lastEnrollment) {
        this.lastEnrollment = lastEnrollment;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getNation() {
        return nation;
    }

    public void setNation(String nation) {
        this.nation = nation;
    }

    public int getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(int academicYear) {
        this.academicYear = academicYear;
    }

    public int getAcademicYearCourse() {
        return academicYearCourse;
    }

    public void setAcademicYearCourse(int academicYearCourse) {
        this.academicYearCourse = academicYearCourse;
    }

    public String getStudentID() {
        return studentID;
    }

    public void setStudentID(String studentID) {
        this.studentID = studentID;
    }

    public int getCodeCourse() {
        return codeCourse;
    }

    public void setCodeCourse(int codeCourse) {
        this.codeCourse = codeCourse;
    }

    public int getTypeStudent() {
        return typeStudent;
    }

    public void setTypeStudent(int typeStudent) {
        this.typeStudent = typeStudent;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public boolean isErasmus() {
        return isErasmus;
    }

    public void setErasmus(boolean erasmus) {
        isErasmus = erasmus;
    }

    public boolean isEnrolled() {
        return getTypeStudent() != -1;
    }

    public String getUserUUID() {
        return userUUID;
    }

    public void setUserUUID(String userUUID) {
        this.userUUID = userUUID;
    }

    public String getNameAlias() {
        return nameAlias;
    }

    public void setNameAlias(String nameAlias) {
        this.nameAlias = nameAlias;
    }

    public String getSurnameAlias() {
        return surnameAlias;
    }

    public void setSurnameAlias(String surnameAlias) {
        this.surnameAlias = surnameAlias;
    }

    public String getPersonalEmail() {
        return personalEmail;
    }

    public void setPersonalEmail(String personalEmail) {
        this.personalEmail = personalEmail;
    }

    public String getDisplayFirstName() {
        String aliasName = this.getNameAlias();
        return (aliasName != null && !aliasName.isEmpty()) ? aliasName : this.getFirstName();
    }

    public String getDisplayLastName() {
        String aliasSurname = this.getSurnameAlias();
        return (aliasSurname != null && !aliasSurname.isEmpty()) ? aliasSurname : this.getLastName();
    }

    @Override
    public String toString() {
        return "Student{" +
                "CF='" + CF + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", birthDate=" + birthDate +
                ", birthCity='" + birthCity + '\'' +
                ", birthPlace='" + birthPlace + '\'' +
                ", courseYear='" + courseYear + '\'' +
                ", firstEnrollment='" + firstEnrollment + '\'' +
                ", lastEnrollment='" + lastEnrollment + '\'' +
                ", departmentName='" + departmentName + '\'' +
                ", courseName='" + courseName + '\'' +
                ", nation='" + nation + '\'' +
                ", email='" + email + '\'' +
                ", citizenship='" + citizenship + '\'' +
                ", gender='" + gender + '\'' +
                ", studentStatus='" + studentStatus + '\'' +
                ", academicYear=" + academicYear +
                ", academicYearCourse=" + academicYearCourse +
                ", studentID=" + studentID +
                ", codeCourse=" + codeCourse +
                ", typeStudent=" + typeStudent +
                ", cfu=" + cfu +
                ", isErasmus=" + isErasmus +
                ", userUUID=" + userUUID +
                ", nameAlias=" + nameAlias +
                ", surnameAlias=" + surnameAlias +
                ", personalEmail=" + personalEmail +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return academicYear == student.academicYear &&
                academicYearCourse == student.academicYearCourse &&
                studentID.equals(student.studentID) &&
                userUUID.equals(student.userUUID) &&
                codeCourse == student.codeCourse &&
                typeStudent == student.typeStudent &&
                cfu == student.cfu &&
                isErasmus == student.isErasmus &&
                Objects.equals(CF, student.CF) &&
                Objects.equals(firstName, student.firstName) &&
                Objects.equals(lastName, student.lastName) &&
                Objects.equals(birthDate, student.birthDate) &&
                Objects.equals(birthCity, student.birthCity) &&
                Objects.equals(birthPlace, student.birthPlace) &&
                Objects.equals(courseYear, student.courseYear) &&
                Objects.equals(firstEnrollment, student.firstEnrollment) &&
                Objects.equals(lastEnrollment, student.lastEnrollment) &&
                Objects.equals(departmentName, student.departmentName) &&
                Objects.equals(courseName, student.courseName) &&
                Objects.equals(nation, student.nation) &&
                Objects.equals(email, student.email) &&
                Objects.equals(citizenship, student.citizenship) &&
                Objects.equals(gender, student.gender) &&
                Objects.equals(studentStatus, student.studentStatus) &&
                Objects.equals(nameAlias, student.nameAlias) &&
                Objects.equals(surnameAlias, student.surnameAlias) &&
                Objects.equals(personalEmail, student.personalEmail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(CF, firstName, lastName, birthDate, birthCity, birthPlace, courseYear, firstEnrollment, lastEnrollment, departmentName, courseName, nation, email, citizenship, gender, studentStatus, academicYear, academicYearCourse, studentID, codeCourse, typeStudent, cfu, isErasmus, userUUID, nameAlias, surnameAlias, personalEmail);
    }
}
