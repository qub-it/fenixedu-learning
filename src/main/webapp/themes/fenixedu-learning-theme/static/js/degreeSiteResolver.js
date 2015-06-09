function resolveSite(folder, url) {
    url = url.substring(folder.functionality.fullPath.length() + 1);
    var parts = url.split('/');
    if(parts.length < 3) { return null; }
    var degreeType = Java.type('org.fenixedu.academic.domain.Degree');
    var degree = degreeType.readBySigla(parts[0]);
    return degree ? degree.site : null;
}
 
function getBaseUrl(folder, site) {
    var degree = site.getDegree();
    return folder.functionality.fullPath.substring(1) + 
           '/' + degree.sigla;
}