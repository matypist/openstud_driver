package matypist.openstud.driver.core.internals;

import matypist.openstud.driver.core.models.Classroom;
import matypist.openstud.driver.core.models.ExamDoable;
import matypist.openstud.driver.core.models.Lesson;
import matypist.openstud.driver.exceptions.OpenstudConnectionException;
import matypist.openstud.driver.exceptions.OpenstudInvalidResponseException;
import org.threeten.bp.LocalDate;

import java.util.List;
import java.util.Map;

public interface ClassroomHandler {
    List<Classroom> getClassRoom(String query, boolean withTimetable) throws OpenstudInvalidResponseException,
            OpenstudConnectionException;

    List<Lesson> getClassroomTimetable(Classroom room, LocalDate date) throws OpenstudConnectionException,
            OpenstudInvalidResponseException;

    List<Lesson> getClassroomTimetable(int id, LocalDate date) throws OpenstudConnectionException, OpenstudInvalidResponseException;

    Map<String, List<Lesson>> getTimetable(List<ExamDoable> exams) throws OpenstudInvalidResponseException,
            OpenstudConnectionException;


}
