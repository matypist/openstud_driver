# OpenStud Driver

[![GitHub tag](https://img.shields.io/github/v/tag/matypist/openstud_driver)](https://github.com/matypist/openstud_driver/releases/latest)
[![Jitpack tag](https://www.jitpack.io/v/matypist/openstud_driver.svg)](https://www.jitpack.io/#matypist/openstud_driver)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

OpenStud Driver is a Java Library to obtain info from Sapienza University's Infostud 2.0.

This library is thread-safe and Android-friendly.

## Getting started

### Prerequisites
This application is written with JDK8 in mind. If you don't have a Java Development Kit installed you can download it from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

### Compile from sources
- `git clone` or download this repo.
- Open a terminal in the directory where the sources are stored.
- Execute `mvn install -DskipTests` . You will find the .jar file in the target folder.

### Add to your project

OpenStud Driver can be easily added to your existing project through Maven or Gradle.

**Maven**

1) Add the JitPack repository
```
<repositories>
	<repository>
	    <id>jitpack.io</id>
	    <url>https://jitpack.io</url>
	</repository>
</repositories>
```
2) Add the dependency
```
<dependency>
    <groupId>com.github.matypist</groupId>
    <artifactId>openstud_driver</artifactId>
    <version>0.61.3</version>
</dependency>
```

**Gradle**

1) Add it in your root build.gradle at the end of repositories:
```
allprojects {
    repositories {
		maven { url 'https://jitpack.io' }
	}
}
```
2) Add the dependency
```
dependencies {
    implementation 'com.github.matypist:openstud_driver:0.61.3'
}
```

## Functionalities
**Authentication**
- [x] Login
- [x] Security question and password recovery
- [x] Passsword reset

**Profile**
- [x] Student infos
- [x] Certificates
- [x] Photo and student card

**Classroom**
- [x] Classroom and timetable

**Exams**
- [x] Doable and done exams
- [x] Active and available reservations
- [x] Insert and delete reservations
- [x] Pdf reservation
- [x] Calendar events
- [x] Course surveys (OPIS)

**Taxes**
- [x] Paid and unpaid taxes
- [x] Current ISEE and ISEE history

**News**
- [x] News and newsletter events

### Examples
```
Logger log = Logger.getLogger("matypist.openstud");

//Create an OpenStud object and sign-in
Openstud os = new OpenstudBuilder().setPassword("myPassword").setStudentID(123456).setLogger(log).build();
os.login();

//Get personal infos about a student
Student st = os.getInfoStudent();

//Get a list of exams that the student hasn't passed yet
List<ExamDoable> doable = os.getExamsDoable();

//Get a list of exams that the student passed with flying colors :)
List<ExamPassed> passed = os.getExamsPassed());

//Get a list of reservations that the student has already placed
List<ExamReservation> active = os.getActiveReservations();

//Get a list of the reservations avaiable for a particular exam
List<ExamReservation> available = os.getAvailableReservations(doable.get(0),st);

//Place a reservation for a particulare session of an exam
Pair<Integer,String> pr = os.insertReservation(available.get(0));

//Download the PDF of a particular active reservation
byte[] pdf = os.getPdf(active.get(0));

//Delete an active reservation
int result = os.deleteReservation(active.get(0));
 ```

## Dependencies
- [Square OkHttp](https://github.com/square/okhttp)
- [JUnit](https://github.com/junit-team/junit4)
- [jsoup](https://jsoup.org/)
- [ThreeTenBP](https://github.com/ThreeTen/threetenbp)
- [org/Json](https://github.com/stleary/JSON-java)
- [Apache HttpComponents](https://hc.apache.org/)
- [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/)
- [Apache Commons-IO](https://commons.apache.org/proper/commons-io/)
