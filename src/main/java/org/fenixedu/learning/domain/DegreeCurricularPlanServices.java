package org.fenixedu.learning.domain;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

// qubExtension
abstract public class DegreeCurricularPlanServices {

    static final public Logger logger = LoggerFactory.getLogger(DegreeCurricularPlanServices.class);

    static public DegreeCurricularPlan getMostRecentDegreeCurricularPlan(final Degree degree,
            final Optional<ExecutionYear> executionYearOptional) {

        return getDegreeCurricularPlansForYear(degree, executionYearOptional).stream()
                .max(Comparator.comparing(DegreeCurricularPlan::getInitialDateYearMonthDay)).orElse(null);
    }

    static public Set<DegreeCurricularPlan> getDegreeCurricularPlansForYear(final Degree degree,
            final Optional<ExecutionYear> yearOptional) {

        final Set<DegreeCurricularPlan> result = Sets.newHashSet();

        if (degree != null) {
            final ExecutionYear year = yearOptional.isPresent() ? yearOptional
                    .get() : getDegreeCurricularPlansExecutionYears(degree).stream().max(ExecutionYear::compareTo).orElse(null);

            if (year != null) {
                degree.getDegreeCurricularPlansSet().stream().filter(predicateDCP(year))
                        .collect(Collectors.toCollection(() -> result));
            }
        }

        return result;
    }

    static private Predicate<DegreeCurricularPlan> predicateDCP(final ExecutionYear year) {
        return plan -> {
            return plan.isApproved() && plan.isActive() && plan.hasExecutionDegreeFor(year)
                    && !plan.getCurricularCoursesWithExecutionIn(year).isEmpty();
        };
    }

    static public Set<ExecutionYear> getDegreeCurricularPlansExecutionYears(final Degree degree) {
        final Set<ExecutionYear> result = Sets.newHashSet();

        if (degree != null) {
            return degree.getDegreeCurricularPlansSet().stream().flatMap(plan -> plan.getExecutionDegreesSet().stream())
                    .filter(ed -> predicateDCP(ed.getExecutionYear()).test(ed.getDegreeCurricularPlan()))
                    .map(ed -> ed.getExecutionYear()).collect(Collectors.toCollection(() -> result));
        }

        return result;
    }

}
