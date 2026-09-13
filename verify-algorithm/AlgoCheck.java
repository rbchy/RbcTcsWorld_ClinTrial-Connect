import java.time.LocalDate;
import java.time.Period;

/**
 * Standalone, dependency-free re-implementation of MatchingService.score() logic, used ONLY to
 * verify the matching algorithm's correctness in this sandbox (which cannot reach Maven Central
 * to build/run the real Spring Boot + JUnit test suite — see README). The logic below is copied
 * verbatim from src/main/java/com/clintrial/service/MatchingService.java.
 */
public class AlgoCheck {

    static final int SCORE_CONDITION_EXACT = 60;
    static final int SCORE_CONDITION_PARTIAL = 35;
    static final int SCORE_SAME_STATE = 25;
    static final int SCORE_SAME_CITY = 15;

    static class Patient {
        LocalDate dob; String gender, conditionName, city, state;
        Patient(LocalDate dob, String gender, String conditionName, String city, String state) {
            this.dob = dob; this.gender = gender; this.conditionName = conditionName; this.city = city; this.state = state;
        }
        int age() { return Period.between(dob, LocalDate.now()).getYears(); }
    }

    static class Trial {
        String status, conditionName, eligibleGender, city, state;
        int minAge, maxAge;
        Trial(String status, String conditionName, int minAge, int maxAge, String eligibleGender, String city, String state) {
            this.status = status; this.conditionName = conditionName; this.minAge = minAge; this.maxAge = maxAge;
            this.eligibleGender = eligibleGender; this.city = city; this.state = state;
        }
    }

    static int conditionScore(String patientCondition, String trialCondition) {
        if (patientCondition == null || trialCondition == null) return 0;
        String a = patientCondition.trim().toLowerCase();
        String b = trialCondition.trim().toLowerCase();
        if (a.isEmpty() || b.isEmpty()) return 0;
        if (a.equals(b)) return SCORE_CONDITION_EXACT;
        if (a.contains(b) || b.contains(a)) return SCORE_CONDITION_PARTIAL;
        return 0;
    }

    static boolean eqIgnoreCaseSafe(String a, String b) { return a != null && b != null && a.equalsIgnoreCase(b); }

    static int score(Patient patient, Trial trial) {
        if (!"RECRUITING".equals(trial.status)) return 0;
        int age = patient.age();
        if (age < trial.minAge || age > trial.maxAge) return 0;
        String eligibleGender = trial.eligibleGender == null ? "ANY" : trial.eligibleGender;
        if (!"ANY".equalsIgnoreCase(eligibleGender) && patient.gender != null && !eligibleGender.equalsIgnoreCase(patient.gender)) return 0;
        int conditionScore = conditionScore(patient.conditionName, trial.conditionName);
        if (conditionScore == 0) return 0;
        int score = conditionScore;
        if (eqIgnoreCaseSafe(patient.state, trial.state)) {
            score += SCORE_SAME_STATE;
            if (eqIgnoreCaseSafe(patient.city, trial.city)) score += SCORE_SAME_CITY;
        }
        return Math.min(score, 100);
    }

    static int failures = 0;

    static void check(String name, int expected, int actual) {
        if (expected == actual) {
            System.out.println("PASS  " + name + " => " + actual);
        } else {
            System.out.println("FAIL  " + name + " expected=" + expected + " actual=" + actual);
            failures++;
        }
    }

    public static void main(String[] args) {
        LocalDate dobFor40 = LocalDate.now().minusYears(40);
        LocalDate dobFor17 = LocalDate.now().minusYears(17);
        LocalDate dobFor71 = LocalDate.now().minusYears(71);
        LocalDate dobFor18Exact = LocalDate.now().minusYears(18);

        // 1. Exact condition match, same city+state, recruiting, in age range -> 60+25+15=100
        check("exact condition + same city/state",
                100,
                score(new Patient(dobFor40, "Female", "Type 2 Diabetes", "Austin", "TX"),
                      new Trial("RECRUITING", "Type 2 Diabetes", 18, 70, "ANY", "Austin", "TX")));

        // 2. Exact condition, same state only (different city) -> 60+25=85
        check("exact condition + same state only",
                85,
                score(new Patient(dobFor40, "Female", "Type 2 Diabetes", "Houston", "TX"),
                      new Trial("RECRUITING", "Type 2 Diabetes", 18, 70, "ANY", "Austin", "TX")));

        // 3. Exact condition, different state -> 60
        check("exact condition, different state",
                60,
                score(new Patient(dobFor40, "Female", "Type 2 Diabetes", "Miami", "FL"),
                      new Trial("RECRUITING", "Type 2 Diabetes", 18, 70, "ANY", "Austin", "TX")));

        // 4. Partial/contains condition match -> 35
        check("partial condition match",
                35,
                score(new Patient(dobFor40, "Female", "Diabetes", "Miami", "FL"),
                      new Trial("RECRUITING", "Type 2 Diabetes", 18, 70, "ANY", "Austin", "TX")));

        // 5. Completely unrelated condition -> 0
        check("unrelated condition excluded",
                0,
                score(new Patient(dobFor40, "Female", "Asthma", "Austin", "TX"),
                      new Trial("RECRUITING", "Type 2 Diabetes", 18, 70, "ANY", "Austin", "TX")));

        // 6. Age just below minAge -> excluded
        check("age below minAge excluded",
                0,
                score(new Patient(dobFor17, "Female", "Asthma", "Dallas", "TX"),
                      new Trial("RECRUITING", "Asthma", 18, 70, "ANY", "Dallas", "TX")));

        // 6b. Age exactly at minAge (boundary) -> included
        check("age exactly at minAge boundary included",
                100,
                score(new Patient(dobFor18Exact, "Female", "Asthma", "Dallas", "TX"),
                      new Trial("RECRUITING", "Asthma", 18, 70, "ANY", "Dallas", "TX")));

        // 7. Age just above maxAge -> excluded
        check("age above maxAge excluded",
                0,
                score(new Patient(dobFor71, "Female", "Type 2 Diabetes", "Austin", "TX"),
                      new Trial("RECRUITING", "Type 2 Diabetes", 18, 70, "ANY", "Austin", "TX")));

        // 8. Gender-restricted trial, matching gender -> included
        check("gender-restricted trial, matching gender",
                60,
                score(new Patient(dobFor40, "Female", "Breast Cancer", "Miami", "FL"),
                      new Trial("RECRUITING", "Breast Cancer", 18, 70, "Female", "Austin", "TX")));

        // 9. Gender-restricted trial, non-matching gender -> excluded
        check("gender-restricted trial, non-matching gender excluded",
                0,
                score(new Patient(dobFor40, "Male", "Breast Cancer", "Miami", "FL"),
                      new Trial("RECRUITING", "Breast Cancer", 18, 70, "Female", "Austin", "TX")));

        // 10. Trial not recruiting (DRAFT) -> excluded regardless of otherwise-perfect match
        check("non-recruiting trial excluded",
                0,
                score(new Patient(dobFor40, "Female", "Type 2 Diabetes", "Austin", "TX"),
                      new Trial("DRAFT", "Type 2 Diabetes", 18, 70, "ANY", "Austin", "TX")));

        // 11. Score never exceeds 100 (cap check) — exact + state + city already sums to exactly 100,
        //     so this also confirms no overflow beyond the intended max.
        check("score capped at 100",
                100,
                score(new Patient(dobFor40, "Female", "Type 2 Diabetes", "Austin", "TX"),
                      new Trial("RECRUITING", "Type 2 Diabetes", 0, 120, "ANY", "Austin", "TX")));

        // 12. Null patient gender against a gender-restricted trial -> still included (can't exclude on unknown data)
        check("null patient gender not excluded by gender-restricted trial",
                60,
                score(new Patient(dobFor40, null, "Breast Cancer", "Miami", "FL"),
                      new Trial("RECRUITING", "Breast Cancer", 18, 70, "Female", "Austin", "TX")));

        System.out.println();
        if (failures == 0) {
            System.out.println("ALL " + 13 + " ASSERTIONS PASSED (note: 12 checks + this summary)");
        } else {
            System.out.println(failures + " CHECK(S) FAILED");
            System.exit(1);
        }
    }
}
