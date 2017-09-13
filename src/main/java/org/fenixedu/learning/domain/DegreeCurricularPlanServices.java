package org.fenixedu.learning.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

        // legidio, HACK until a better effort comes
        return getDegreeCurricularPlansForYear(degree, executionYearOptional).stream()
                .max(Comparator.comparing(DegreeCurricularPlan::getInitialDateYearMonthDay)).orElse(null);
    }

    static public Set<DegreeCurricularPlan> getDegreeCurricularPlansForYear(final Degree degree,
            final Optional<ExecutionYear> yearOptional) {

        final Set<DegreeCurricularPlan> result = Sets.newHashSet();

        if (degree != null) {
            final ExecutionYear year = yearOptional.isPresent() ? yearOptional.get() : degree
                    .getDegreeCurricularPlansExecutionYears().stream().max(ExecutionYear::compareTo).orElse(null);

            if (year != null) {
                final List<DegreeCurricularPlan> dcps = degree.getDegreeCurricularPlansForYear(year);
                dcps.stream().filter(
                        plan -> plan.isApproved() && plan.isActive() && !plan.getCurricularCoursesWithExecutionIn(year).isEmpty())
                        .collect(Collectors.toCollection(() -> result));
            }
        }

        return result;
    }
}
