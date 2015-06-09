function resolveSite(folder, url) {
    url = url.substring(folder.functionality.fullPath.length() + 1);
    var parts = url.split('/');
    if(parts.length < 3) { return null; }
    var interval = getInterval(parts);
    var courseType = Java.type('org.fenixedu.academic.domain.ExecutionCourse');
    var course = interval === null ? courseType.readLastBySigla(parts[0]) : courseType.readLastByExecutionIntervalAndSigla(parts[0], interval);
    return course ? course.site : null;
}
 
function getInterval(parts) {
    if (parts[1].matches('[1-9][0-9]{3}-[1-9][0-9]{3}')) {
        var year = Java.type('org.fenixedu.academic.domain.ExecutionYear').readExecutionYearByName(parts[1].replace('-', '/'));
        if (year && parts[2].matches('[1-2]-semestre')) {
            return year.getExecutionSemesterFor(parts[2].charAt(0));
        }
        return year;
    }
    return null;
}
 
function getBaseUrl(folder, site) {
    var course = site.getExecutionCourse();
    return folder.functionality.fullPath.substring(1) + 
           '/' + course.sigla +
           '/' + course.executionPeriod.executionYear.year.replace('/', '-') +
           '/' + course.executionPeriod.semester + '-semestre';
}