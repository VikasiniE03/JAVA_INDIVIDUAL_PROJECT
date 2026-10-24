import java.math.BigDecimal;
import java.math.RoundingMode;

public final class EvaluationEngine {
    public enum CourseType {
        THEORY(40, 60, 45),
        PRACTICAL(60, 40, 50),
        EMBEDDED(50, 50, 45);

        private final int internalWeight;
        private final int externalWeight;
        private final int eseMinimumRaw;

        CourseType(int internalWeight, int externalWeight, int eseMinimumRaw) {
            this.internalWeight = internalWeight;
            this.externalWeight = externalWeight;
            this.eseMinimumRaw = eseMinimumRaw;
        }

        public int internalWeight() {
            return internalWeight;
        }

        public int externalWeight() {
            return externalWeight;
        }

        public int eseMinimumRaw() {
            return eseMinimumRaw;
        }

        public static CourseType from(String value) {
            try {
                return CourseType.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Course type must be THEORY, PRACTICAL, or EMBEDDED.");
            }
        }
    }

    public record Result(
            BigDecimal attendancePercent,
            BigDecimal internalScore,
            BigDecimal internalWeighted,
            BigDecimal eseWeighted,
            BigDecimal total,
            String grade,
            String status,
            int gradePoint,
            boolean complete) {
    }

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private EvaluationEngine() {
    }

    public static Result evaluate(
            CourseType type,
            int classesHeld,
            int classesAttended,
            BigDecimal cia1,
            BigDecimal cia2,
            BigDecimal phase2,
            BigDecimal eseMark,
            boolean eseAbsent) {
        if (classesHeld < 0 || classesAttended < 0 || classesAttended > classesHeld) {
            throw new IllegalArgumentException("Attendance counts must satisfy 0 <= attended <= held.");
        }
        BigDecimal attendance = classesHeld == 0
                ? null
                : BigDecimal.valueOf(classesAttended).multiply(HUNDRED)
                        .divide(BigDecimal.valueOf(classesHeld), 2, RoundingMode.HALF_UP);
        long attendanceHundredths = (long) classesAttended * 100;
        long shortageCutoff = (long) classesHeld * 65;
        long eligibleCutoff = (long) classesHeld * 75;

        BigDecimal internal = average(cia1, cia2, phase2);
        BigDecimal internalWeighted = internal == null ? null
                : weighted(internal, type.internalWeight());

        if (attendance == null) {
            return result(null, internal, internalWeighted, null, null, "PENDING",
                    "ATTENDANCE NOT RECORDED", 0, false);
        }
        if (attendanceHundredths < shortageCutoff) {
            return result(attendance, internal, internalWeighted, null, null, "SA",
                    "SHORTAGE OF ATTENDANCE", 0, true);
        }
        if (attendanceHundredths < eligibleCutoff) {
            return result(attendance, internal, internalWeighted, null, null, "PENDING",
                    "CONDITIONAL - MEDICAL CONDONATION REQUIRED (NOT IMPLEMENTED)", 0, false);
        }
        if (eseAbsent) {
            BigDecimal total = internalWeighted == null ? null : internalWeighted;
            return result(attendance, internal, internalWeighted, BigDecimal.ZERO, total,
                    "AB", "ABSENT IN ESE", 0, true);
        }
        if (eseMark == null || internalWeighted == null) {
            return result(attendance, internal, internalWeighted, null, null, "PENDING",
                    "ASSESSMENT MARKS OR ESE MARK PENDING", 0, false);
        }

        BigDecimal eseWeighted = weighted(eseMark, type.externalWeight());
        BigDecimal total = internalWeighted.add(eseWeighted);
        boolean eseMinimumMet = eseMark.compareTo(BigDecimal.valueOf(type.eseMinimumRaw())) >= 0;
        if (!eseMinimumMet || total.compareTo(BigDecimal.valueOf(50)) < 0) {
            return result(attendance, internal, internalWeighted, eseWeighted, total,
                    "RA", "FAIL", 0, true);
        }

        String grade = gradeFor(total);
        return result(attendance, internal, internalWeighted, eseWeighted, total,
                grade, "PASS", gradePointFor(grade), true);
    }

    public static BigDecimal semesterGpa(Iterable<SemesterCourse> courses) {
        BigDecimal weightedPoints = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        for (SemesterCourse course : courses) {
            weightedPoints = weightedPoints.add(course.credits().multiply(BigDecimal.valueOf(course.result().gradePoint())));
            totalCredits = totalCredits.add(course.credits());
        }
        if (totalCredits.signum() == 0) {
            return null;
        }
        return weightedPoints.divide(totalCredits, 2, RoundingMode.HALF_UP);
    }

    public record SemesterCourse(BigDecimal credits, Result result) {
    }

    private static BigDecimal average(BigDecimal first, BigDecimal second, BigDecimal third) {
        if (first == null || second == null || third == null) {
            return null;
        }
        return first.add(second).add(third).divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal weighted(BigDecimal score, int weight) {
        return score.multiply(BigDecimal.valueOf(weight))
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private static String gradeFor(BigDecimal total) {
        if (total.compareTo(BigDecimal.valueOf(91)) >= 0) return "O";
        if (total.compareTo(BigDecimal.valueOf(81)) >= 0) return "A+";
        if (total.compareTo(BigDecimal.valueOf(71)) >= 0) return "A";
        if (total.compareTo(BigDecimal.valueOf(61)) >= 0) return "B+";
        return "B";
    }

    private static int gradePointFor(String grade) {
        return switch (grade) {
            case "O" -> 10;
            case "A+" -> 9;
            case "A" -> 8;
            case "B+" -> 7;
            case "B" -> 6;
            default -> 0;
        };
    }

    private static Result result(
            BigDecimal attendance,
            BigDecimal internal,
            BigDecimal internalWeighted,
            BigDecimal eseWeighted,
            BigDecimal total,
            String grade,
            String status,
            int gradePoint,
            boolean complete) {
        return new Result(attendance, internal, internalWeighted, eseWeighted,
                total, grade, status, gradePoint, complete);
    }
}
